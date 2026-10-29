# Canos brancos — correção 1.4.2

A captura enviada mostra fundo, personagem e carros normais, mas os dois canos inteiramente brancos. A inspeção identificou que `createPipeColorFilter(4)` usava `PorterDuffColorFilter(0xFFFFFFFF, SRC_ATOP)`: ele substitui o RGB dos pixels opacos pela cor branca, descartando os detalhes da textura.

Na versão 1.4.2, a opção branca converte para luminância e adiciona 40 níveis de brilho, mantendo contraste e alpha. Funciona com seleção fixa e com a opção aleatória, sem apagar preferências ou recordes.

O modo de software da 1.4.1 foi removido: ele não corrigia o filtro. A aceleração gráfica está novamente habilitada também no Android 17. A decodificação explícita em ARGB e a reinicialização do Paint por quadro permanecem.

Teste de regressão compila o método de filtros real e verifica contraste entre aço escuro, médio e claro, além da preservação da transparência. A validação no aparelho do usuário continua necessária.
