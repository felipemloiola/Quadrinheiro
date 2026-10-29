package com.example.data

import com.example.model.ComicEntry
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

/**
 * Tradutor e normalizador de metadados de quadrinhos e mangás para Português (PT-BR).
 * Garante que títulos, sinopses, editoras, formatos e nomes de séries vindos de APIs
 * internacionais (Apple Books US, Google Books, Open Library, Kitsu, MangaDex, AniList)
 * sejam sempre exibidos em português, sem edições duplicadas em outros idiomas e sem
 * nomes de lojas ("Apple Books", "Google Books") no lugar da editora real.
 */
object PortugueseComicTranslator {

    data class CachedComicTranslation(
        val titlePtBr: String = "",
        val synopsisPtBr: String = "",
        val isFinalSynopsis: Boolean = false
    )

    private val synopsisTranslationCache = ConcurrentHashMap<String, String>()
    private val titleTranslationCache = ConcurrentHashMap<String, String>()
    private val canonicalWorkKeyCache = ConcurrentHashMap<String, String>()
    private val comicIdTranslationCache = ConcurrentHashMap<String, CachedComicTranslation>()

    @Volatile
    var onTranslationPersisted: ((ComicEntry) -> Unit)? = null

    // Palavras exclusivas de edições alemãs, francesas ou italianas que poluem buscas de HQs
    private val foreignEditionNoiseRegex = Regex(
        """(?i)\b(?:der\s+beginn\s+einer|er\s+hat\s+nicht|band\s+\d+|bd\.\s*\d+|sammelband|ausgabe|panini\s+verlags|auf\s+deutsch|édition\s+intégrale|tome\s+\d+|edizione\s+italiana|fumetti)\b"""
    )

    private val bundleNoiseRegex = Regex(
        """(?i)\b(?:box\s+set|slipcase|bundle|collection\s+vol\.\s*\d+[-–]\d+|vols?\.?\s*\d+\s*[-–]\s*\d+|issues\s*#?\d+\s*[-–]\s*#?\d+)\b"""
    )

    private val germanSynopsisIndicators = Regex(
        """(?i)\b(?:der|die|das|und|nicht|einer|einem|einen|ist|mit|auf|für|von|dem|den|sich|auch|wird|wurde|hat|haben)\b"""
    )

    private val englishSynopsisIndicators = Regex(
        """(?i)\b(?:the|and|with|from|that|this|his|her|their|into|after|before|when|where|while|against|between|through|collects|collecting|issues|written\s+by|illustrated\s+by|new\s+york\s+times|bestselling|storyline|chapter\s+one|caped\s+crusader|dark\s+knight|gotham\s+city)\b"""
    )

    private val portugueseSynopsisIndicators = Regex(
        """(?i)\b(?:para|como|uma|pelo|pela|dos|das|nos|nas|seu|sua|seus|suas|mais|quando|onde|após|entre|contra|história|edição|publicada|escrita|ilustrada|quadrinhos|capítulo|cidade|herói|mundo)\b"""
    )

    private val parentheticalYearBeforeIssueRegex = Regex(
        """\s*\(\d{4}\s*(?:[-–]\s*\d{0,4})?\)\s*(?=#\d+|Vol\.|Volume|Ed\.)""",
        RegexOption.IGNORE_CASE
    )
    private val parentheticalEditionNoiseRegex = Regex(
        """(?i)\s*\((?:english|german|french|spanish|italian|us|uk)?\s*(?:edition|version|tpb|hardcover| paperback|ebook|digital|comics?)\)"""
    )
    private val bilingualDuplicateParentheticalRegex = Regex(
        """(?i)\s*\(\s*(?:Kingdom\s+Come|Civil\s+War|Invincible|The\s+Dark\s+Knight\s+Returns|Year\s+One|Born\s+Again|The\s+Killing\s+Joke)\s*\)"""
    )
    private val whitespaceRegex = Regex("""\s+""")
    private val hashIssueDetectRegex = Regex("""#\s*\d+\b""")
    private val ptAccentsRegex = Regex("""[ãõçáéíóúâêô]""", RegexOption.IGNORE_CASE)
    private val htmlTagRegex = Regex("<[^>]*>?")
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val canonicalEditionStripRegex = Regex(
        """(?i)\b(?:edicao\s+definitiva|edicao\s+de\s+luxo|edicao\s+absoluta|edicao\s+de\s+aniversario|edicao\s+de\s+colecionador|capa\s+dura|encadernado|tpb|hardcover|deluxe|omnibus|compendio|vizbig|completo|the\s+perfect\s+edition)\b"""
    )
    private val wordUmSuffixRegex = Regex("""\b(?:ano|volume|vol|livro|parte|edicao|ed)\s+um\b""")
    private val anoUmNormalizedRegex = Regex("""\bano\s+um_1\b""")
    private val volUmNormalizedRegex = Regex("""\b(?:volume|vol)\s+um_1\b""")
    private val livroUmNormalizedRegex = Regex("""\blivro\s+um_1\b""")
    private val volNumberNormalizedRegex = Regex("""\b(?:volume|livro|tomo|band|bd)\s*(\d+)\b""")
    private val nonAlphaNumSpaceRegex = Regex("""[^a-z0-9\s]""")

    private val catalogByWorkKey: Map<String, List<ComicEntry>> by lazy {
        CuratedCatalog.curatedComicsCatalog.groupBy { canonicalWorkKey(it.title) }
    }

    // Mapeamento direto de títulos e subtítulos consagrados EN/DE -> PT-BR
    private val canonicalTitleReplacements: List<Pair<Regex, String>> = listOf(
        // Batman
        Regex("""(?i)\bAbsolute\s+Batman\s*[-–—:]?\s*(?:Bd\.|Band|Vol\.?|Volume|Book|#)?\s*0*(\d+)\b""") to "Batman Absoluto #$1",
        Regex("""(?i)\bAbsolute\s+Batman\b""") to "Batman Absoluto",
        Regex("""(?i)\bBatman\s*:\s*Year\s+One\b""") to "Batman: Ano Um",
        Regex("""(?i)\bYear\s+One\b""") to "Ano Um",
        Regex("""(?i)\bBatman\s*:\s*Zero\s+Year\b""") to "Batman: Ano Zero",
        Regex("""(?i)\bZero\s+Year\b""") to "Ano Zero",
        Regex("""(?i)\bBatman\s*:\s*The\s+Dark\s+Knight\s+Returns\b""") to "Batman: O Cavaleiro das Trevas",
        Regex("""(?i)\bThe\s+Dark\s+Knight\s+Returns\b""") to "O Cavaleiro das Trevas",
        Regex("""(?i)\bBatman\s*:\s*The\s+Dark\s+Knight\s+Strikes\s+Again\b""") to "Batman: O Cavaleiro das Trevas 2",
        Regex("""(?i)\bThe\s+Dark\s+Knight\s+Strikes\s+Again\b""") to "O Cavaleiro das Trevas 2",
        Regex("""(?i)\bThe\s+Dark\s+Knight\s+III\s*:\s*The\s+Master\s+Race\b""") to "O Cavaleiro das Trevas 3: A Raça Superior",
        Regex("""(?i)\bThe\s+Master\s+Race\b""") to "A Raça Superior",
        Regex("""(?i)\bThe\s+Dark\s+Knight\s+Returns\s*:\s*The\s+Golden\s+Child\b""") to "O Cavaleiro das Trevas: A Criança Dourada",
        Regex("""(?i)\bBatman\s*:\s*The\s+Killing\s+Joke\b""") to "Batman: A Piada Mortal",
        Regex("""(?i)\bThe\s+Killing\s+Joke\b""") to "A Piada Mortal",
        Regex("""(?i)\bBatman\s*:\s*The\s+Long\s+Halloween\b""") to "Batman: O Longo Dia das Bruxas",
        Regex("""(?i)\bThe\s+Long\s+Halloween\b""") to "O Longo Dia das Bruxas",
        Regex("""(?i)\bBatman\s*:\s*Dark\s+Victory\b""") to "Batman: Vitória Sombria",
        Regex("""(?i)\bDark\s+Victory\b""") to "Vitória Sombria",
        Regex("""(?i)\bBatman\s*:\s*Hush\b""") to "Batman: Silêncio",
        Regex("""(?i)\bBatman\s*:\s*The\s+Court\s+of\s+Owls\b""") to "Batman: A Corte das Corujas",
        Regex("""(?i)\bThe\s+Court\s+of\s+Owls\b""") to "A Corte das Corujas",
        Regex("""(?i)\bCourt\s+of\s+Owls\b""") to "A Corte das Corujas",
        Regex("""(?i)\bBatman\s*:\s*City\s+of\s+Owls\b""") to "Batman: A Cidade das Corujas",
        Regex("""(?i)\bCity\s+of\s+Owls\b""") to "A Cidade das Corujas",
        Regex("""(?i)\bDeath\s+of\s+the\s+Family\b""") to "Morte da Família",
        Regex("""(?i)\bA\s+Death\s+in\s+the\s+Family\b""") to "Morte em Família",
        Regex("""(?i)\bUnder\s+the\s+Red\s+Hood\b""") to "Sob o Capuz",
        Regex("""(?i)\bBatman\s*:\s*Knightfall\b""") to "Batman: A Queda do Morcego",
        Regex("""(?i)\bKnightfall\b""") to "A Queda do Morcego",
        Regex("""(?i)\bBatman\s*:\s*White\s+Knight\b""") to "Batman: Cavaleiro Branco",
        Regex("""(?i)\bCurse\s+of\s+the\s+White\s+Knight\b""") to "A Maldição do Cavaleiro Branco",
        Regex("""(?i)\bArkham\s+Asylum\b""") to "Asilo Arkham",
        Regex("""(?i)\bBatman\s*:\s*Earth\s+One\b""") to "Batman: Terra Um",
        Regex("""(?i)\bEarth\s+One\b""") to "Terra Um",
        Regex("""(?i)\bThree\s+Jokers\b""") to "Os Três Coringas",
        Regex("""(?i)\bThe\s+Man\s+Who\s+Laughs\b""") to "O Homem Que Ri",
        Regex("""(?i)\bGargoyle\s+of\s+Gotham\b""") to "A Gárgula de Gotham",
        Regex("""(?i)\bThe\s+Black\s+Mirror\b""") to "Espelho Sombrio",
        Regex("""(?i)\bBatman\s*:\s*Endgame\b""") to "Batman: Fim de Jogo",
        Regex("""(?i)\bBatman\s*:\s*Damned\b""") to "Batman: Amaldiçoado",
        Regex("""(?i)\bBatman\s*:\s*The\s+Imposter\b""") to "Batman: O Impostor",
        Regex("""(?i)\bAll[- ]Star\s+Batman\s*(?:&|and)\s*Robin(?:,\s*the\s+Boy\s+Wonder)?\b""") to "Grandes Astros: Batman & Robin",
        Regex("""(?i)\bAll[- ]Star\s+Batman\b""") to "Grandes Astros: Batman",
        Regex("""(?i)\bI\s+Am\s+Gotham\b""") to "Eu Sou Gotham",
        Regex("""(?i)\bI\s+Am\s+Suicide\b""") to "Eu Sou Suicida",
        Regex("""(?i)\bI\s+Am\s+Bane\b""") to "Eu Sou Bane",
        Regex("""(?i)\bThe\s+War\s+of\s+Jokes\s+and\s+Riddles\b""") to "A Guerra das Piadas e Charadas",
        Regex("""(?i)\bFailsafe\b""") to "Contingência",

        // Superman & DC
        Regex("""(?i)\bAll[- ]Star\s+Superman\b""") to "Grandes Astros: Superman",
        Regex("""(?i)\bSuperman\s*:\s*Red\s+Son\b""") to "Superman: Entre a Foice e o Martelo",
        Regex("""(?i)\bRed\s+Son\b""") to "Entre a Foice e o Martelo",
        Regex("""(?i)\bSuperman\s*:\s*For\s+All\s+Seasons\b""") to "Superman: As Quatro Estações",
        Regex("""(?i)\bSuperman\s*:\s*Birthright\b""") to "Superman: O Legado das Estrelas",
        Regex("""(?i)\bSuperman\s*:\s*Secret\s+Origin\b""") to "Superman: Origem Secreta",
        Regex("""(?i)\bSuperman\s*:\s*American\s+Alien\b""") to "Superman: Alienígena Americano",
        Regex("""(?i)\bSuperman\s*:\s*Up\s+in\s+the\s+Sky\b""") to "Superman: No Alto do Céu",
        Regex("""(?i)\bWhatever\s+Happened\s+to\s+the\s+Man\s+of\s+Tomorrow\??\b""") to "O Que Aconteceu ao Homem do Amanhã?",
        Regex("""(?i)\bFor\s+the\s+Man\s+Who\s+Has\s+Everything\b""") to "Para o Homem Que Tem Tudo",
        Regex("""(?i)\bSupergirl\s*:\s*Woman\s+of\s+Tomorrow\b""") to "Supergirl: A Mulher do Amanhã",
        Regex("""(?i)\bWoman\s+of\s+Tomorrow\b""") to "A Mulher do Amanhã",
        Regex("""(?i)\bKingdom\s+Come\b""") to "Reino do Amanhã",
        Regex("""(?i)\bJustice\s+League\b""") to "Liga da Justiça",
        Regex("""(?i)\bCrisis\s+on\s+Infinite\s+Earths\b""") to "Crise nas Infinitas Terras",
        Regex("""(?i)\bInfinite\s+Crisis\b""") to "Crise Infinita",
        Regex("""(?i)\bFinal\s+Crisis\b""") to "Crise Final",
        Regex("""(?i)\bIdentity\s+Crisis\b""") to "Crise de Identidade",
        Regex("""(?i)\bFlashpoint\b""") to "Ponto de Ignição",
        Regex("""(?i)\bGreen\s+Lantern\s*:\s*Rebirth\b""") to "Lanterna Verde: Renascimento",
        Regex("""(?i)\bGreen\s+Lantern\b""") to "Lanterna Verde",
        Regex("""(?i)\bGreen\s+Arrow\b""") to "Arqueiro Verde",
        Regex("""(?i)\bWonder\s+Woman\b""") to "Mulher-Maravilha",
        Regex("""(?i)\bNightwing\b""") to "Asa Noturna",
        Regex("""(?i)\bCatwoman\b""") to "Mulher-Gato",
        Regex("""(?i)\bHarley\s+Quinn\b""") to "Arlequina",
        Regex("""(?i)\bTeen\s+Titans\b""") to "Jovens Titãs",
        Regex("""(?i)\bMister\s+Miracle\b""") to "Senhor Milagre",
        Regex("""(?i)\bStrange\s+Adventures\b""") to "Estranhas Aventuras",
        Regex("""(?i)\bThe\s+New\s+Frontier\b""") to "A Nova Fronteira",

        // Vertigo / Sandman / Alan Moore
        Regex("""(?i)\bThe\s+Sandman\b""") to "Sandman",
        Regex("""(?i)\bPreludes\s*(?:&|and)\s*Nocturnes\b""") to "Prelúdios e Noturnos",
        Regex("""(?i)\bThe\s+Doll'?s\s+House\b""") to "A Casa de Bonecas",
        Regex("""(?i)\bDream\s+Country\b""") to "Terra dos Sonhos",
        Regex("""(?i)\bSeason\s+of\s+Mists\b""") to "Estação das Brumas",
        Regex("""(?i)\bA\s+Game\s+of\s+You\b""") to "Um Jogo de Você",
        Regex("""(?i)\bFables\s*(?:&|and)\s*Reflections\b""") to "Fábulas e Reflexões",
        Regex("""(?i)\bBrief\s+Lives\b""") to "Vidas Breves",
        Regex("""(?i)\bWorlds'?\s+End\b""") to "Fim dos Mundos",
        Regex("""(?i)\bThe\s+Kindly\s+Ones\b""") to "As Benevolentes",
        Regex("""(?i)\bThe\s+Wake\b""") to "O Despertar",
        Regex("""(?i)\bEndless\s+Nights\b""") to "Noites Sem Fim",
        Regex("""(?i)\bSandman\s*:\s*Overture\b""") to "Sandman: Prelúdio",
        Regex("""(?i)\bV\s+for\s+Vendetta\b""") to "V de Vingança",
        Regex("""(?i)\bSaga\s+of\s+the\s+Swamp\s+Thing\b""") to "A Saga do Monstro do Pântano",
        Regex("""(?i)\bSwamp\s+Thing\b""") to "Monstro do Pântano",
        Regex("""(?i)\bY\s*:\s*The\s+Last\s+Man\b""") to "Y: O Último Homem",
        Regex("""(?i)\bFrom\s+Hell\b""") to "Do Inferno",
        Regex("""(?i)\bThe\s+League\s+of\s+Extraordinary\s+Gentlemen\b""") to "A Liga Extraordinária",

        // Marvel
        Regex("""(?i)\bThe\s+Amazing\s+Spider[- ]Man\b""") to "O Espetacular Homem-Aranha",
        Regex("""(?i)\bAmazing\s+Spider[- ]Man\b""") to "O Espetacular Homem-Aranha",
        Regex("""(?i)\bUltimate\s+Spider[- ]Man\b""") to "Homem-Aranha Ultimate",
        Regex("""(?i)\bSpectacular\s+Spider[- ]Man\b""") to "O Espetacular Homem-Aranha",
        Regex("""(?i)\bSpider[- ]Man\s*:\s*Kraven'?s\s+Last\s+Hunt\b""") to "Homem-Aranha: A Última Caçada de Kraven",
        Regex("""(?i)\bKraven'?s\s+Last\s+Hunt\b""") to "A Última Caçada de Kraven",
        Regex("""(?i)\bSpider[- ]Man\s*:\s*Blue\b""") to "Homem-Aranha: Azul",
        Regex("""(?i)\bSpider[- ]Man\s*:\s*Life\s+Story\b""") to "Homem-Aranha: História de Vida",
        Regex("""(?i)\bSpider[- ]Man\b""") to "Homem-Aranha",
        Regex("""(?i)\bDaredevil\s*:\s*Born\s+Again\b""") to "Demolidor: A Queda de Murdock",
        Regex("""(?i)\bBorn\s+Again\b""") to "A Queda de Murdock",
        Regex("""(?i)\bDaredevil\s*:\s*The\s+Man\s+Without\s+Fear\b""") to "Demolidor: O Homem Sem Medo",
        Regex("""(?i)\bThe\s+Man\s+Without\s+Fear\b""") to "O Homem Sem Medo",
        Regex("""(?i)\bDaredevil\s*:\s*Yellow\b""") to "Demolidor: Amarelo",
        Regex("""(?i)\bDaredevil\b""") to "Demolidor",
        Regex("""(?i)\bThe\s+Dark\s+Phoenix\s+Saga\b""") to "A Saga da Fênix Negra",
        Regex("""(?i)\bDark\s+Phoenix\s+Saga\b""") to "A Saga da Fênix Negra",
        Regex("""(?i)\bDays\s+of\s+Future\s+Past\b""") to "Dias de um Futuro Esquecido",
        Regex("""(?i)\bGod\s+Loves,\s*Man\s+Kills\b""") to "Deus Ama, o Homem Mata",
        Regex("""(?i)\bAstonishing\s+X[- ]Men\b""") to "Surpreendentes X-Men",
        Regex("""(?i)\bNew\s+X[- ]Men\b""") to "Novos X-Men",
        Regex("""(?i)\bHouse\s+of\s+X\b""") to "Dinastia X",
        Regex("""(?i)\bPowers\s+of\s+X\b""") to "Potências de X",
        Regex("""(?i)\bHouse\s+of\s+M\b""") to "Dinastia M",
        Regex("""(?i)\bHawkeye\b""") to "Gavião Arqueiro",
        Regex("""(?i)\bMy\s+Life\s+as\s+a\s+Weapon\b""") to "Minha Vida Como Uma Arma",
        Regex("""(?i)\bLittle\s+Hits\b""") to "Pequenos Acertos",
        Regex("""(?i)\bCivil\s+War\b""") to "Guerra Civil",
        Regex("""(?i)\bInfinity\s+Gauntlet\b""") to "Desafio Infinito",
        Regex("""(?i)\bSecret\s+Wars\b""") to "Guerras Secretas",
        Regex("""(?i)\bOld\s+Man\s+Logan\b""") to "Velho Logan",
        Regex("""(?i)\bThe\s+Immortal\s+Hulk\b""") to "O Imortal Hulk",
        Regex("""(?i)\bImmortal\s+Hulk\b""") to "O Imortal Hulk",
        Regex("""(?i)\bPlanet\s+Hulk\b""") to "Planeta Hulk",
        Regex("""(?i)\bWorld\s+War\s+Hulk\b""") to "Hulk Contra o Mundo",
        Regex("""(?i)\bCaptain\s+America\b""") to "Capitão América",
        Regex("""(?i)\bWinter\s+Soldier\b""") to "O Soldado Invernal",
        Regex("""(?i)\bIron\s+Man\b""") to "Homem de Ferro",
        Regex("""(?i)\bFantastic\s+Four\b""") to "Quarteto Fantástico",
        Regex("""(?i)\bNew\s+Avengers\b""") to "Novos Vingadores",
        Regex("""(?i)\bThe\s+Avengers\b""") to "Os Vingadores",
        Regex("""(?i)\bAvengers\b""") to "Vingadores",
        Regex("""(?i)\bMoon\s+Knight\b""") to "Cavaleiro da Lua",
        Regex("""(?i)\bDoctor\s+Strange\b""") to "Doutor Estranho",
        Regex("""(?i)\bBlack\s+Panther\b""") to "Pantera Negra",
        Regex("""(?i)\bSilver\s+Surfer\b""") to "Surfista Prateado",
        Regex("""(?i)\bThe\s+Punisher\b""") to "O Justiceiro",

        // Image / Dark Horse / Outros
        Regex("""(?i)\bInvincible\b""") to "Invencível",
        Regex("""(?i)\bSeed\s+of\s+Destruction\b""") to "Sementes da Destruição",
        Regex("""(?i)\bThe\s+Complete\s+Maus\b""") to "Maus: A História de um Sobrevivente",
        Regex("""(?i)\bA\s+Survivor'?s\s+Tale\b""") to "A História de um Sobrevivente",
        Regex("""(?i)\bThe\s+Complete\s+Persepolis\b""") to "Persépolis: Completo",

        // Marcadores de edição e volume
        Regex("""(?i)\s*[-–—]?\s*\b(?:Bd\.|Band|Tome)\s*0*(\d+)\b""") to " — Vol. $1",
        Regex("""(?i)\bIssue\s*#?\s*0*(\d+)\b""") to "#$1",
        Regex("""(?i)\bBook\s+One\b""") to "Livro Um",
        Regex("""(?i)\bBook\s+Two\b""") to "Livro Dois",
        Regex("""(?i)\bBook\s+Three\b""") to "Livro Três",
        Regex("""(?i)\bBook\s+Four\b""") to "Livro Quatro",
        Regex("""(?i)\bBook\s+0*(\d+)\b""") to "Livro $1",
        Regex("""(?i)\bVolume\s+One\b""") to "Volume Um",
        Regex("""(?i)\bVolume\s+Two\b""") to "Volume 2",
        Regex("""(?i)\bVolume\s+Three\b""") to "Volume 3",
        Regex("""(?i)\bVolume\s+Four\b""") to "Volume 4",
        Regex("""(?i)\bPart\s+One\b""") to "Parte 1",
        Regex("""(?i)\bPart\s+Two\b""") to "Parte 2",
        Regex("""(?i)\bPart\s+0*(\d+)\b""") to "Parte $1",
        Regex("""(?i)\bChapter\s+One\b""") to "Capítulo 1",
        Regex("""(?i)\bChapter\s+0*(\d+)\b""") to "Capítulo $1",
        Regex("""(?i)\bThe\s+Perfect\s+Edition\b""") to "Edição Definitiva",
        Regex("""(?i)\bDeluxe\s+Edition\b""") to "Edição de Luxo",
        Regex("""(?i)\bAbsolute\s+Edition\b""") to "Edição Absoluta",
        Regex("""(?i)\bCollector'?s\s+Edition\b""") to "Edição de Colecionador",
        Regex("""(?i)\bAnniversary\s+Edition\b""") to "Edição de Aniversário",
        Regex("""(?i)\bCompendium\b""") to "Compêndio",
        Regex("""(?i)\bOrigins\b""") to "Origens"
    )

    /**
     * Detecta se um título ou descrição veio em alemão, francês ou italiano ou é um box/pacote repetido.
     */
    fun isUnwantedForeignOrBundleResult(rawTitle: String, rawSynopsis: String = ""): Boolean {
        val t = rawTitle.trim()
        if (foreignEditionNoiseRegex.containsMatchIn(t)) return true
        if (bundleNoiseRegex.containsMatchIn(t)) return true
        if (rawSynopsis.isNotBlank()) {
            val germanMatches = germanSynopsisIndicators.findAll(rawSynopsis).count()
            if (germanMatches >= 4) return true
        }
        return false
    }

    /**
     * Traduz e limpa o título de qualquer quadrinho para Português (PT-BR).
     */
    fun translateTitleToPtBr(rawTitle: String): String {
        val trimmed = rawTitle.trim()
        if (trimmed.isEmpty()) return "Sem Título"
        titleTranslationCache[trimmed]?.let { return it }

        var working = trimmed
            .replace(parentheticalYearBeforeIssueRegex, " ")
            .replace(parentheticalEditionNoiseRegex, "")

        for ((regex, replacement) in canonicalTitleReplacements) {
            working = regex.replace(working, replacement)
        }

        working = working
            .replace(bilingualDuplicateParentheticalRegex, "")
            .replace(whitespaceRegex, " ")
            .trim()

        titleTranslationCache[trimmed] = working
        return working
    }

    /**
     * Infere a editora real da HQ/Mangá sem jamais exibir "Apple Books", "Google Books", "Open Library", "MangaDex" ou "Kitsu".
     */
    fun normalizePublisher(
        rawPublisher: String,
        title: String,
        authors: String,
        genre: String
    ): String {
        val pClean = rawPublisher.trim()
        val combined = "${title.lowercase()} ${authors.lowercase()} ${pClean.lowercase()} ${genre.lowercase()}"

        val invalidStoreNames = listOf(
            "apple books", "itunes", "google books", "open library",
            "kitsu", "mangadex", "anilist", "tankobon / internacional",
            "panini verlags", "unknown", "editora", "sem editora", "null"
        )
        val isStorePlaceholder = pClean.isBlank() || invalidStoreNames.any { pClean.lowercase().contains(it) }

        if (!isStorePlaceholder) {
            return when {
                pClean.equals("DC", ignoreCase = true) || pClean.contains("DC Entertainment", ignoreCase = true) || pClean.contains("DC Comics", ignoreCase = true) -> "DC Comics"
                pClean.contains("Vertigo", ignoreCase = true) -> "Vertigo / DC"
                pClean.equals("Marvel", ignoreCase = true) || pClean.contains("Marvel", ignoreCase = true) -> "Marvel Comics"
                pClean.contains("Image", ignoreCase = true) -> "Image Comics"
                pClean.contains("Dark Horse", ignoreCase = true) -> "Dark Horse Comics"
                pClean.contains("Kodansha", ignoreCase = true) -> "Kodansha / Panini"
                pClean.contains("Shueisha", ignoreCase = true) || pClean.contains("Jump", ignoreCase = true) -> "Shueisha / Panini"
                pClean.contains("VIZ", ignoreCase = true) -> "VIZ Media / Panini"
                pClean.contains("Panini", ignoreCase = true) -> "Panini Comics"
                else -> pClean
            }
        }

        return when {
            combined.contains("sandman") || combined.contains("preacher") || combined.contains("hellblazer") ||
                combined.contains("constantine") || combined.contains("monstro do pântano") || combined.contains("swamp thing") ||
                combined.contains("v de vingança") || combined.contains("vendetta") || combined.contains("daytripper") ||
                combined.contains("último homem") || combined.contains("last man") || combined.contains("fábulas") -> "Vertigo / DC"

            combined.contains("batman") || combined.contains("superman") || combined.contains("watchmen") ||
                combined.contains("mulher-maravilha") || combined.contains("wonder woman") || combined.contains("lanterna verde") ||
                combined.contains("green lantern") || combined.contains("flash") || combined.contains("aquaman") ||
                combined.contains("liga da justiça") || combined.contains("justice league") || combined.contains("asa noturna") ||
                combined.contains("nightwing") || combined.contains("coringa") || combined.contains("joker") ||
                combined.contains("arlequina") || combined.contains("harley") || combined.contains("gotham") ||
                combined.contains("reino do amanhã") || combined.contains("kingdom come") || combined.contains("senhor milagre") ||
                combined.contains("supergirl") || combined.contains("dc / vertigo") -> "DC Comics"

            combined.contains("homem-aranha") || combined.contains("spider-man") || combined.contains("spiderman") ||
                combined.contains("x-men") || combined.contains("demolidor") || combined.contains("daredevil") ||
                combined.contains("vingadores") || combined.contains("avengers") || combined.contains("wolverine") ||
                combined.contains("velho logan") || combined.contains("gavião arqueiro") || combined.contains("hawkeye") ||
                combined.contains("guerra civil") || combined.contains("civil war") || combined.contains("hulk") ||
                combined.contains("homem de ferro") || combined.contains("iron man") || combined.contains("capitão américa") ||
                combined.contains("captain america") || combined.contains("thor") || combined.contains("quarteto fantástico") ||
                combined.contains("cavaleiro da lua") || combined.contains("moon knight") || combined.contains("justiceiro") ||
                combined.contains("punisher") || combined.contains("deadpool") || combined.contains("marvel") -> "Marvel Comics"

            combined.contains("saga") || combined.contains("spawn") || combined.contains("invencível") ||
                combined.contains("invincible") || combined.contains("walking dead") || combined.contains("kirkman") ||
                combined.contains("mcfarlane") || combined.contains("brian k. vaughan") -> "Image Comics"

            combined.contains("hellboy") || combined.contains("mignola") || combined.contains("sin city") ||
                combined.contains("umbrella academy") || combined.contains("black hammer") -> "Dark Horse Comics"

            combined.contains("berserk") -> "Dark Horse / Panini"
            combined.contains("akira") || combined.contains("vinland saga") || combined.contains("attack on titan") ||
                combined.contains("shingeki") -> "Kodansha / Panini"
            combined.contains("one piece") || combined.contains("vagabond") || combined.contains("naruto") ||
                combined.contains("dragon ball") || combined.contains("jujutsu") || combined.contains("chainsaw") ||
                combined.contains("bleach") || combined.contains("death note") || combined.contains("slam dunk") -> "Shueisha / Panini"
            combined.contains("monster") || combined.contains("urasawa") || combined.contains("uzumaki") ||
                combined.contains("junji ito") -> "Panini / JBC"
            combined.contains("maus") || combined.contains("persépolis") || combined.contains("persepolis") -> "Companhia das Letras"
            genre.equals("Mangá", ignoreCase = true) -> "Panini Mangás"
            else -> "Panini Comics"
        }
    }

    /**
     * Infere o gênero padronizado da estante ("DC / Vertigo", "Marvel", "Mangá", "Graphic Novel").
     */
    fun normalizeGenre(rawGenre: String, title: String, publisher: String, authors: String): String {
        val combined = "${rawGenre.lowercase()} ${title.lowercase()} ${publisher.lowercase()} ${authors.lowercase()}"
        return when {
            rawGenre == "Mangá" || combined.contains("mangá") || combined.contains("manga") ||
                combined.contains("tankobon") || combined.contains("shueisha") || combined.contains("kodansha") ||
                combined.contains("berserk") || combined.contains("akira") || combined.contains("vagabond") ||
                combined.contains("one piece") || combined.contains("uzumaki") || combined.contains("vinland saga") -> "Mangá"

            combined.contains("dc comics") || combined.contains("vertigo") || combined.contains("batman") ||
                combined.contains("superman") || combined.contains("watchmen") || combined.contains("sandman") ||
                combined.contains("mulher-maravilha") || combined.contains("lanterna verde") || combined.contains("flash") ||
                combined.contains("liga da justiça") || combined.contains("monstro do pântano") || combined.contains("v de vingança") ||
                combined.contains("reino do amanhã") || combined.contains("asa noturna") || combined.contains("coringa") ||
                combined.contains("daytripper") -> "DC / Vertigo"

            combined.contains("marvel") || combined.contains("homem-aranha") || combined.contains("spider-man") ||
                combined.contains("x-men") || combined.contains("demolidor") || combined.contains("daredevil") ||
                combined.contains("vingadores") || combined.contains("avengers") || combined.contains("gavião arqueiro") ||
                combined.contains("hawkeye") || combined.contains("hulk") || combined.contains("wolverine") ||
                combined.contains("thor") || combined.contains("capitão américa") || combined.contains("homem de ferro") ||
                combined.contains("guerra civil") -> "Marvel"

            else -> "Graphic Novel"
        }
    }

    /**
     * Infere o formato em português e corrige a contagem de páginas (para que revistas mensais #1, #2 não fiquem com 160 págs).
     */
    fun normalizeFormatAndPages(
        rawFormat: String,
        rawPages: Int,
        title: String,
        genre: String
    ): Pair<String, Int> {
        val tLower = title.lowercase()
        val hasHashIssue = hashIssueDetectRegex.containsMatchIn(title) &&
            !tLower.contains("vol.") && !tLower.contains("volume") && !tLower.contains("livro") &&
            !tLower.contains("edição de luxo") && !tLower.contains("edição definitiva")

        val format = when {
            genre == "Mangá" -> "Mangá (Tankobon)"
            tLower.contains("edição de luxo") || tLower.contains("edição definitiva") ||
                tLower.contains("edição absoluta") || tLower.contains("capa dura") || tLower.contains("omnibus") -> "Edição de Luxo (Capa Dura)"
            hasHashIssue -> "Revista Mensal (Issue)"
            rawFormat.isNotBlank() -> rawFormat
            else -> "Encadernado (TPB)"
        }

        val pages = when {
            hasHashIssue && (rawPages <= 0 || rawPages == 160) -> 36
            rawPages > 0 -> rawPages
            genre == "Mangá" -> 200
            else -> 160
        }

        return format to pages
    }

    /**
     * Verifica se o texto já está em português.
     */
    fun isPortugueseText(text: String): Boolean {
        val clean = text.trim()
        if (clean.isEmpty()) return true
        val ptCount = portugueseSynopsisIndicators.findAll(clean).count()
        val enCount = englishSynopsisIndicators.findAll(clean).count()
        val deCount = germanSynopsisIndicators.findAll(clean).count()
        if (deCount >= 3 || enCount >= 3) return false
        if (ptCount >= 2) return true
        val hasPtAccents = ptAccentsRegex.containsMatchIn(clean)
        return hasPtAccents && enCount == 0 && deCount == 0
    }

    /**
     * Traduz uma sinopse estrangeira (inglês, alemão, etc.) para Português (PT-BR).
     * Utiliza o catálogo curado quando disponível (busca O(1)), ou o endpoint rápido de tradução com cache em memória,
     * e garante fallback descritivo em português caso offline.
     */
    fun translateSynopsisToPtBr(
        rawSynopsis: String,
        translatedTitle: String,
        authors: String,
        publisher: String,
        releaseYear: Int,
        pageCount: Int,
        issueNumber: Int = 0,
        allowNetworkTranslation: Boolean = false
    ): String {
        val clean = rawSynopsis
            .replace(htmlTagRegex, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(whitespaceRegex, " ")
            .trim()

        // 1. Se já está em português e não é apenas um placeholder curto, mantém imediatamente
        if (clean.isNotBlank() && isPortugueseText(clean) && !clean.contains("Apple Books") && !clean.contains("Google Books")) {
            return clean
        }

        // 2. Verifica correspondência O(1) no catálogo curado em português
        val workKey = canonicalWorkKey(translatedTitle)
        val candidates = catalogByWorkKey[workKey]
        val targetPrimary = SeriesSequenceEngine.extractPrimaryAuthor(authors)
        val catalogHit = candidates?.firstOrNull { cat ->
            val issueMatch = (issueNumber <= 0 && SeriesSequenceEngine.inferIssueNumber(cat) <= 0) ||
                (issueNumber > 0 && SeriesSequenceEngine.inferIssueNumber(cat) == issueNumber)
            val catPrimary = SeriesSequenceEngine.extractPrimaryAuthor(cat.authors)
            val authorMatch = targetPrimary.isBlank() || catPrimary.isBlank() ||
                catPrimary.equals(targetPrimary, ignoreCase = true) ||
                cat.authors.contains(targetPrimary, ignoreCase = true) ||
                authors.contains(catPrimary, ignoreCase = true)
            issueMatch && authorMatch
        }

        if (catalogHit != null && catalogHit.synopsis.isNotBlank()) {
            return catalogHit.synopsis
        }

        // 3. Verifica cache ou traduz via HTTP se permitido (em chamadas de rede em background)
        if (clean.length >= 20) {
            val cacheKey = clean.take(180)
            synopsisTranslationCache[cacheKey]?.let { return it }

            if (allowNetworkTranslation) {
                val translatedOnline = tryTranslateViaHttp(clean.take(550))
                if (!translatedOnline.isNullOrBlank() && isPortugueseText(translatedOnline)) {
                    synopsisTranslationCache[cacheKey] = translatedOnline
                    return translatedOnline
                }
            }
        }

        // 4. Fallback em português rico e contextualizado (nunca deixa inglês ou alemão aparecer na interface)
        val cleanAuthors = authors.ifBlank { "autores consagrados" }
        return if (issueNumber > 0) {
            "Edição #$issueNumber da série $translatedTitle, escrita e ilustrada por $cleanAuthors e publicada pela editora $publisher ($releaseYear, $pageCount páginas)."
        } else {
            "$translatedTitle é uma obra em quadrinhos escrita e ilustrada por $cleanAuthors, publicada pela editora $publisher ($releaseYear, aproximadamente $pageCount páginas)."
        }
    }

    private fun tryTranslateViaHttp(text: String): String? {
        return try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=pt&dt=t&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()
            MultiApiSearchEngine.sharedHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val outer = JSONArray(body)
                val sentences = outer.optJSONArray(0) ?: return null
                val sb = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val seg = sentences.optJSONArray(i)?.optString(0, "") ?: ""
                    sb.append(seg)
                }
                sb.toString().replace(whitespaceRegex, " ").trim().takeIf { it.isNotBlank() }
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Gera uma chave canônica de obra (independente de idioma ou tipo de edição como Deluxe/TPB/Definitiva)
     * para impedir que a mesma HQ apareça repetida (ex: "Batman: Ano Um", "Batman: Year One", "Batman: Ano Um - Edição Definitiva").
     */
    fun canonicalWorkKey(rawTitle: String): String {
        val trimmed = rawTitle.trim()
        if (trimmed.isEmpty()) return ""
        canonicalWorkKeyCache[trimmed]?.let { return it }

        val ptTitle = translateTitleToPtBr(trimmed).lowercase()
        val withoutAccents = Normalizer.normalize(ptTitle, Normalizer.Form.NFD)
            .replace(diacriticsRegex, "")

        val stripped = withoutAccents
            .replace(canonicalEditionStripRegex, " ")
            .replace(wordUmSuffixRegex, "$0_1")
            .replace(anoUmNormalizedRegex, "ano 1")
            .replace(volUmNormalizedRegex, "vol 1")
            .replace(livroUmNormalizedRegex, "livro 1")
            .replace(volNumberNormalizedRegex, "vol $1")
            .replace(nonAlphaNumSpaceRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()

        canonicalWorkKeyCache[trimmed] = stripped
        return stripped
    }

    fun resolveComicCacheKey(entry: ComicEntry): String {
        val primaryId = entry.id.trim()
        if (primaryId.isNotEmpty()) return primaryId
        val extId = entry.externalId.trim()
        if (extId.isNotEmpty()) return extId
        return canonicalWorkKey(entry.title)
    }

    fun getCachedTranslationForComicId(comicId: String): CachedComicTranslation? {
        if (comicId.isBlank()) return null
        return comicIdTranslationCache[comicId]
    }

    /**
     * Pré-carrega traduções já salvas no SafeStorage ou Firestore para garantir que
     * o mesmo `comicId` nunca seja traduzido duas vezes.
     */
    fun seedPersistedTranslations(
        comics: List<ComicEntry>,
        persistedMap: Map<String, Pair<String, String>> = emptyMap()
    ) {
        persistedMap.forEach { (key, pair) ->
            val (tPt, sPt) = pair
            if (key.isNotBlank() && (tPt.isNotBlank() || sPt.isNotBlank())) {
                comicIdTranslationCache[key] = CachedComicTranslation(
                    titlePtBr = tPt,
                    synopsisPtBr = sPt,
                    isFinalSynopsis = sPt.isNotBlank()
                )
            }
        }
        val allSeedComics = comics + CuratedCatalog.curatedComicsCatalog
        allSeedComics.forEach { c ->
            val key = resolveComicCacheKey(c)
            val extKey = c.externalId.trim()
            val tPt = c.titlePtBr.ifBlank {
                if (c.id.startsWith("cat-") || c.id.startsWith("init-")) c.title else ""
            }
            val sPt = c.synopsisPtBr.ifBlank {
                if ((c.id.startsWith("cat-") || c.id.startsWith("init-") || isPortugueseText(c.synopsis)) &&
                    c.synopsis.isNotBlank() &&
                    !c.synopsis.contains("Apple Books") &&
                    !c.synopsis.contains("Google Books")
                ) {
                    c.synopsis
                } else {
                    ""
                }
            }
            if (tPt.isNotBlank() || sPt.isNotBlank()) {
                val cached = CachedComicTranslation(
                    titlePtBr = tPt.ifBlank { translateTitleToPtBr(c.title) },
                    synopsisPtBr = sPt,
                    isFinalSynopsis = sPt.isNotBlank()
                )
                if (key.isNotBlank()) comicIdTranslationCache[key] = cached
                if (extKey.isNotBlank()) comicIdTranslationCache[extKey] = cached
            }
        }
    }

    private fun recordComicTranslation(
        entry: ComicEntry,
        titlePtBr: String,
        synopsisPtBr: String,
        isFinalSynopsis: Boolean
    ) {
        val key = resolveComicCacheKey(entry)
        val extKey = entry.externalId.trim()
        val cached = CachedComicTranslation(
            titlePtBr = titlePtBr,
            synopsisPtBr = synopsisPtBr,
            isFinalSynopsis = isFinalSynopsis
        )
        if (key.isNotBlank()) comicIdTranslationCache[key] = cached
        if (extKey.isNotBlank()) comicIdTranslationCache[extKey] = cached
    }

    /**
     * Tradução Leve / Batch (usada pelo `MultiApiSearchEngine.kt` e listagens de sequências):
     * Traduz apenas título, formato, editora e gênero se necessário.
     * NÃO dispara a tradução de sinopses para os 20–50 resultados da busca de uma só vez;
     * reutiliza `synopsisPtBr` apenas se já estiver em cache para este `comicId`.
     */
    fun localizeComicMetadataOnly(entry: ComicEntry): ComicEntry {
        val key = resolveComicCacheKey(entry)
        val extKey = entry.externalId.trim()
        val cached = comicIdTranslationCache[key] ?: if (extKey.isNotBlank()) comicIdTranslationCache[extKey] else null

        val ptTitle = when {
            entry.titlePtBr.isNotBlank() -> entry.titlePtBr
            cached != null && cached.titlePtBr.isNotBlank() -> cached.titlePtBr
            else -> translateTitleToPtBr(entry.title)
        }

        val genre = normalizeGenre(entry.genre, ptTitle, entry.publisher, entry.authors)
        val publisher = normalizePublisher(entry.publisher, ptTitle, entry.authors, genre)
        val (format, pages) = normalizeFormatAndPages(entry.format, entry.pageCount, ptTitle, genre)
        val issueNum = if (entry.issueNumber > 0) entry.issueNumber else SeriesSequenceEngine.inferIssueNumber(entry.copy(title = ptTitle))
        val ptSeries = if (entry.seriesName.isNotBlank()) {
            translateTitleToPtBr(entry.seriesName)
        } else {
            SeriesSequenceEngine.inferBaseSeriesTitle(entry.copy(title = ptTitle))
        }

        val knownSynopsisPtBr = when {
            entry.synopsisPtBr.isNotBlank() -> entry.synopsisPtBr
            cached != null && cached.isFinalSynopsis && cached.synopsisPtBr.isNotBlank() -> cached.synopsisPtBr
            entry.synopsis.isNotBlank() && isPortugueseText(entry.synopsis) &&
                !entry.synopsis.contains("Apple Books") && !entry.synopsis.contains("Google Books") -> entry.synopsis
            else -> ""
        }

        if (cached == null || cached.titlePtBr.isBlank() || (knownSynopsisPtBr.isNotBlank() && !cached.isFinalSynopsis)) {
            recordComicTranslation(
                entry = entry,
                titlePtBr = ptTitle,
                synopsisPtBr = knownSynopsisPtBr,
                isFinalSynopsis = knownSynopsisPtBr.isNotBlank()
            )
        }

        return entry.copy(
            title = ptTitle,
            titlePtBr = ptTitle,
            publisher = publisher,
            genre = genre,
            format = format,
            pageCount = pages,
            currentPage = if (entry.status == com.example.model.ReadingStatus.READ) pages else entry.currentPage.coerceAtMost(pages),
            synopsis = if (knownSynopsisPtBr.isNotBlank()) knownSynopsisPtBr else entry.synopsis,
            synopsisPtBr = knownSynopsisPtBr,
            seriesName = ptSeries,
            issueNumber = issueNum
        )
    }

    /**
     * Localiza um `ComicEntry` completo para Português (PT-BR) sob demanda (Lazy):
     * - Nunca traduz duas vezes o mesmo `comicId` (retorna imediatamente se `synopsisPtBr` já existir).
     * - Assim que traduzir a sinopse ou título, salva no `SafeStorage` e no `QuadrinheiroCloudRepository` via `onTranslationPersisted`.
     */
    fun localizeComic(entry: ComicEntry, allowNetworkTranslation: Boolean = false): ComicEntry {
        val key = resolveComicCacheKey(entry)
        val extKey = entry.externalId.trim()
        val cached = comicIdTranslationCache[key] ?: if (extKey.isNotBlank()) comicIdTranslationCache[extKey] else null

        val ptTitle = when {
            entry.titlePtBr.isNotBlank() -> entry.titlePtBr
            cached != null && cached.titlePtBr.isNotBlank() -> cached.titlePtBr
            else -> translateTitleToPtBr(entry.title)
        }

        val genre = normalizeGenre(entry.genre, ptTitle, entry.publisher, entry.authors)
        val publisher = normalizePublisher(entry.publisher, ptTitle, entry.authors, genre)
        val (format, pages) = normalizeFormatAndPages(entry.format, entry.pageCount, ptTitle, genre)
        val issueNum = if (entry.issueNumber > 0) entry.issueNumber else SeriesSequenceEngine.inferIssueNumber(entry.copy(title = ptTitle))
        val ptSeries = if (entry.seriesName.isNotBlank()) {
            translateTitleToPtBr(entry.seriesName)
        } else {
            SeriesSequenceEngine.inferBaseSeriesTitle(entry.copy(title = ptTitle))
        }

        // 1. Verifica se este comicId já possui sinopse traduzida definitiva (nunca traduz duas vezes o mesmo comicId!)
        val existingFinalSynopsis = when {
            entry.synopsisPtBr.isNotBlank() -> entry.synopsisPtBr
            cached != null && cached.isFinalSynopsis && cached.synopsisPtBr.isNotBlank() -> cached.synopsisPtBr
            else -> ""
        }

        if (existingFinalSynopsis.isNotBlank()) {
            if (cached == null || !cached.isFinalSynopsis) {
                recordComicTranslation(entry, ptTitle, existingFinalSynopsis, isFinalSynopsis = true)
            }
            return entry.copy(
                title = ptTitle,
                titlePtBr = ptTitle,
                publisher = publisher,
                genre = genre,
                format = format,
                pageCount = pages,
                currentPage = if (entry.status == com.example.model.ReadingStatus.READ) pages else entry.currentPage.coerceAtMost(pages),
                synopsis = existingFinalSynopsis,
                synopsisPtBr = existingFinalSynopsis,
                seriesName = ptSeries,
                issueNumber = issueNum
            )
        }

        // 2. Realiza a tradução da sinopse sob demanda
        val rawClean = entry.synopsis
            .replace(htmlTagRegex, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(whitespaceRegex, " ")
            .trim()

        val ptSynopsis = translateSynopsisToPtBr(
            rawSynopsis = entry.synopsis,
            translatedTitle = ptTitle,
            authors = entry.authors,
            publisher = publisher,
            releaseYear = entry.releaseYear,
            pageCount = pages,
            issueNumber = issueNum,
            allowNetworkTranslation = allowNetworkTranslation
        )

        // Determina se a tradução já é definitiva (veio do catálogo curado, já estava em PT-BR,
        // a sinopse original era vazia, ou já executou a chamada de tradução de rede)
        val workKey = canonicalWorkKey(ptTitle)
        val hasCatalogHit = catalogByWorkKey[workKey]?.any { it.synopsis == ptSynopsis } == true
        val isAlreadyPt = rawClean.isNotBlank() && isPortugueseText(rawClean) &&
            !rawClean.contains("Apple Books") && !rawClean.contains("Google Books")
        val isCachedSentence = rawClean.length >= 20 && synopsisTranslationCache.containsKey(rawClean.take(180))
        val isFinal = allowNetworkTranslation || hasCatalogHit || isAlreadyPt || isCachedSentence || rawClean.length < 20

        val finalSynopsisPtBr = if (isFinal) ptSynopsis else ""

        recordComicTranslation(
            entry = entry,
            titlePtBr = ptTitle,
            synopsisPtBr = if (isFinal) ptSynopsis else (cached?.synopsisPtBr ?: ""),
            isFinalSynopsis = isFinal
        )

        val localizedEntry = entry.copy(
            title = ptTitle,
            titlePtBr = ptTitle,
            publisher = publisher,
            genre = genre,
            format = format,
            pageCount = pages,
            currentPage = if (entry.status == com.example.model.ReadingStatus.READ) pages else entry.currentPage.coerceAtMost(pages),
            // Preserva a sinopse bruta em `synopsis` enquanto `isFinal == false` para que a passagem assíncrona com rede traduza o texto real
            synopsis = if (isFinal) ptSynopsis else entry.synopsis,
            synopsisPtBr = finalSynopsisPtBr.ifBlank { ptSynopsis },
            seriesName = ptSeries,
            issueNumber = issueNum
        )

        if (isFinal) {
            // Persiste imediatamente a tradução no SafeStorage e no QuadrinheiroCloudRepository
            val toPersist = localizedEntry.copy(
                synopsis = ptSynopsis,
                synopsisPtBr = ptSynopsis
            )
            recordComicTranslation(toPersist, ptTitle, ptSynopsis, isFinalSynopsis = true)
            onTranslationPersisted?.invoke(toPersist)
            return toPersist
        }

        // Quando allowNetworkTranslation == false e o texto original ainda aguarda a tradução de rede em background,
        // mantemos synopsisPtBr vazio no cache definitivo, mas exibimos o resumo em PT-BR sem perder o rawSynopsis em `entry.synopsis`
        return localizedEntry.copy(synopsisPtBr = "")
    }
}
