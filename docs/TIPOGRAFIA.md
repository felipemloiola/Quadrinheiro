# Tipografia: adaptação Android das diretrizes Apple

Referências oficiais:
- https://developer.apple.com/design/human-interface-guidelines/typography
- https://developer.apple.com/fonts/

O app não incorpora SF Pro, San Francisco ou arquivos de fontes proprietárias da Apple. Usa FontFamily.SansSerif nativa do Android. A aparência depende do sistema/aparelho e não é idêntica à do iOS.

A hierarquia do tema usa referências como corpo 17/22, título grande 34/41 e legendas de 11 ou mais, adaptadas para sp Android. Não se afirma equivalência física entre pt e sp. Não se copiam valores de tracking específicos da SF Pro para outra fonte.

Todos os 15 papéis do Typography usam uma única família. Os nomes antigos PlusJakartaSansFamily e SpaceGroteskTabularFamily são aliases de compatibilidade para a mesma família nativa. O segundo nome não promete, por si só, algarismos tabulares. Os TTF anteriores não são apagados nesta migração.

O tema usa a escala Android multiplicada pela preferência interna exatamente uma vez. Negrito deixa de adicionar sombra e aumento de tamanho artificial. A configuração de peso do sistema é preservada quando o negrito interno está desligado.

Limitação: telas que já especificam fontSize ou lineHeight diretamente mantêm seus valores. Esta etapa unifica a família e a hierarquia do tema; não certifica que todos os textos existentes já atendem aos mínimos recomendados nem que todos os layouts suportam qualquer escala.

Antes de considerar concluído: verificar compilação, executar TypographyPolicyTest e demais testes, revisar truncamentos em escala ampliada e comparar telas claras/escuras em aparelho real. Gestos, tab bar e dados não são alterados por este patch.
