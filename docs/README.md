# Site de apresentação — Corre Nengue

**Endereço GitHub Pages:** https://walmirramos.github.io/FlappyNengue/

Landing page em português, com direção visual inspirada em anúncios de produtos:
tipografia ampla, espaço em branco, aparelho ilustrativo e seções de apresentação.
HTML, CSS e JavaScript sem dependências ou etapa de build.

## Abrir a prévia

Na raiz do repositório, execute:

```powershell
python -m http.server 8765 --bind 127.0.0.1 --directory docs
```

Abra `http://127.0.0.1:8765/`. Use um servidor HTTP para que as prévias em Canvas
possam ler as imagens locais; não abra o HTML pelo protocolo `file://`.

## O que a página contém

- Abertura com uma visualização ilustrativa da partida dentro de um aparelho.
- São Paulo e a viatura com a arte que já pertence ao projeto.
- Prévia dos poderes banana/Sayajin, celular e bazuca, selecionáveis também pelo teclado.
- Paletas original, azul e rosa, com a mesma troca de canais de `CharacterSprites.java`.
- Música e sirene reais do pack 1.7; só tocam após ação explícita e param ao ocultar a página.
- Download local do APK 1.10.1, SHA-256, perguntas frequentes e links para o projeto.
- Layout responsivo, foco visível e respeito à preferência de movimento reduzido.

As animações são demonstrações visuais para apresentar os recursos do APK. A página
não executa o jogo Android no navegador. As imagens e os sons usados são cópias
dos recursos existentes do jogo; o aplicativo Android não foi alterado por este site.

## Hospedagem

O diretório `docs` é a raiz estática completa, com caminhos relativos compatíveis
com a publicação de um repositório em subdiretório no GitHub Pages. Há um
`.nojekyll` para servir os arquivos diretamente. Também pode ser hospedado em
qualquer servidor de arquivos estáticos. Não exige conta de visitante ou backend.

O site é publicado pelo GitHub Pages a partir da pasta `docs` da branch `main`
do repositório `walmirramos/FlappyNengue`. Não é preciso baixar os arquivos
para visitar a página. Alterações nessa pasta são publicadas pelo GitHub.

## Atualizar o APK

Copie o novo APK e seu `.sha256` para `docs/downloads/`. Atualize os links de
download, a versão e o tamanho em `index.html`. Os downloads incluídos são os
mesmos APKs entregues com o projeto; não são links para uma loja de aplicativos.

## Arquivos

- `index.html`: conteúdo, navegação e metadados.
- `styles.css`: layout, animações e media queries.
- `site.js`: demonstrações em Canvas, cores, abas e áudio.
- `assets/`: personagem, cidade, fonte, favicon e sons.
- `assets/corre-nengue.jpg`: capa enviada pelo usuário, usada no README e no compartilhamento.
- `downloads/`: APK 1.10.1 e seu hash.
- `LICENSE.md` e `legal/`: índice e termos dos componentes.
