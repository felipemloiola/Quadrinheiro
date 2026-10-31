# Quadrinheiro

<p align="left">
  <img src="https://github.com/felipemloiola/Quadrinheiro/raw/main/branding/quadrinheiro-logo.svg" alt="Quadrinheiro Logo" width="120" />
</p>

> O ecossistema definitivo para gerenciar, catalogar e acompanhar sua coleção de quadrinhos, graphic novels e mangás com design Liquid Glass, sincronização em nuvem e inteligência de busca.

O **Quadrinheiro** é um aplicativo Android nativo construído com Jetpack Compose, desenhado com acabamento translúcido inspirado na linguagem estética do iOS e otimizado para alta performance com aceleração de hardware e renderização em shaders AGSL.

![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%2B%20Firestore-FFCA28?logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)

---

## Destaques do Aplicativo

- **Apresentação de Boas-Vindas (Onboarding Apple-Style):**
  - Fluxo introdutório exclusivo de primeira inicialização com layout simétrico, logotipo com escalonamento por matriz de transformação (`withTransform`) e transição suave com física de mola.
  - Apresenta os pilares do app: Acervo Pessoal, Sequências e Recomendações, Metas de Leitura e Busca Multi-API na Nuvem.

- **Design e Materiais Ópticos Liquid Glass:**
  - Superfícies translúcidas com refração de borda, meniscos de vidro, aberração cromática e desfoque óptico progressivo via shaders AGSL.
  - Fundo atmosférico que extrai dinamicamente as cores dominantes da capa da HQ em leitura ativa para iluminar a interface.
  - Paleta com temas dinâmicos (Azul Oceano, Vermelho Carmim, Verde Esmeralda, Roxo Imperial, Laranja Solar, Rosa Neon, Turquesa e Obsidian/Prata).

- **Gestão Completa de Acervo e Estante:**
  - Status de leitura em tempo real: **Lendo**, **Lido** e **Quero Ler (Fila)**.
  - Visualizações em Grade (2, 3 ou 4 colunas), Lista Compacta ou Sequências Cronológicas (#1, #2, #3...).
  - Efeito de lombada 3D nos cards, exibição de notas e progresso percentual sobre as capas.

- **Busca Multi-API em Tempo Real:**
  - Motor integrado de busca que consulta simultaneamente Google Books, MangaUpdates e acervos internacionais.
  - Localização automática de títulos, autores e tradução de sinopses para Português (PT-BR) com cache local persistente.
  - Cadastro instantâneo na estante com um único toque ou via formulário manual completo.

- **Metas de Leitura e Diário Cronológico:**
  - Anéis de atividade com anel externo (meta anual de obras) e anel interno (páginas diárias).
  - Diário cronológico com histórico de leituras, datas de conclusão, contador de releituras, avaliação por estrelas, tags rápidas e resenhas detalhadas de até 5.000 caracteres.

- **Autenticação Híbrida e Nuvem Offline-First:**
  - Suporte a Login com Google (via Android Credential Manager + Firebase Auth), E-mail/Senha com hash SHA-256 e Modo Convidado 100% isolado e funcional offline.
  - Sincronização em segundo plano via Cloud Firestore com detecção e resolução de dados pendentes (*dirty state*).

---

## Arquitetura e Engenharia de Performance

| Componente | Implementação |
|---|---|
| **Linguagem & UI** | Kotlin 2.1+, Jetpack Compose, Material 3, Backdrop AGSL Shaders |
| **Aceleração Gráfica** | Aceleração por hardware explícita (`android:hardwareAccelerated="true"`), `largeHeap` para texturas e captura em passagem única (`recordLayer`) |
| **Carregamento de Imagens** | Coil com `HardwareBuffer` direto na VRAM da GPU, cache em disco e memória L1/L2 |
| **Persistência Local** | `SafeStorage` com partição isolada por conta/convidado e exportação de backup autônomo (ZIP/HTML) |
| **Backend & Cloud** | Firebase Authentication, Google Identity Credential Manager, Cloud Firestore |
| **Pipeline de CI/CD** | GitHub Actions com emulador Android (API 34) e testes de regressão de toque e equivalência de pixels |

---

## Estrutura do Repositório

```text
├── app/
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt                  # Ponto de entrada, inicialização de Coil e roteamento
│   │   ├── data/                            # SafeStorage, AuthManager, CloudRepository e APIs
│   │   ├── model/                           # Modelos imutáveis, AppSettings e estados de UI
│   │   ├── ui/
│   │   │   ├── components/                  # QuadrinheiroLogo, LiquidGlass, BookCover e botões
│   │   │   ├── screens/                     # WelcomeIntroScreen, AuthScreen, MainTabs e Ajustes
│   │   │   └── theme/                       # Temas Liquid Glass, paletas Apple e tipografia
│   │   └── viewmodel/                       # QuadrinheiroViewModel e sincronização reativa
│   └── src/androidTest/                     # Testes instrumentados de equivalência de pixels
├── .github/
│   ├── workflows/                           # Workflows automatizados de CI/CD e release de APK
│   └── scripts/                             # Scripts de validação e patches auditáveis
├── firestore.rules                          # Regras de segurança do Firestore
└── README.md
```

---

## Execução Local

### Pré-requisitos
- Android Studio Ladybug ou superior
- Android SDK 36 e JDK 21 (Temurin)
- Projeto Firebase configurado com Google Sign-In e Cloud Firestore

### Instruções

```bash
# Clone o repositório
git clone https://github.com/felipemloiola/Quadrinheiro.git
cd Quadrinheiro

# Configure as credenciais do Firebase
cp /caminho/para/seu/google-services.json app/google-services.json

# Execute a compilação do APK de depuração
./gradlew assembleDebug

# Execute os testes unitários
./gradlew testDebugUnitTest
```

---

## Licença

Todos os direitos reservados. O código-fonte está disponível publicamente apenas para fins de referência e demonstração de portfólio. Não é permitida a reprodução, modificação ou redistribuição sem autorização prévia por escrito.

---

## Autor

**Felipe Loiola** ([@felipemloiola](https://github.com/felipemloiola))
