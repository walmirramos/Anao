# Identidade visual — Corre Nengue

## Direção

São Paulo em pixel art, um protagonista de óculos escuros e a assinatura
**Corre Nengue · A polícia vem aí**. A apresentação usa tipografia ampla,
composição limpa e destaque para as imagens do jogo.

## Artes

| Recurso | Local | Uso |
| --- | --- | --- |
| Capa enviada pelo usuário | `docs/assets/corre-nengue.jpg` | Apresentação do repositório e compartilhamento do site |
| Protagonista | `app/src/main/res/drawable-nodpi/nengue_character.png` | Personagem, prévias e ícone |
| São Paulo | `app/src/main/res/drawable-nodpi/sao_paulo.png` | Cenários diurno e noturno |
| Cidade e viatura | `app/src/main/res/drawable-nodpi/loading_city_police.png` | Abertura, loading e site |
| Logo | `app/src/main/res/drawable-nodpi/corre_logo.png` | Abertura, loading e menu |

A capa foi copiada exatamente como recebida. As ilustrações de cidade e logo
foram criadas para o projeto. As paredes, o piso, parte dos cenários e os efeitos
são desenhados em Canvas com geometria de pixels.

## Paletas e escala

As três paletas do personagem são clássica, azul e rosa. A troca de canais de cor
preserva a transparência, os contornos e as proporções do rosto.
Sprites são escalados sem interpolação para manter os pixels definidos.

## Som e interface

O pack sonoro em uso é o da versão 1.7: música chiptune, sirene, colisões,
rajada de queda e efeitos de Sayajin. A interface do aplicativo utiliza a
tipografia pixel atual do jogo; o site utiliza fontes do sistema nos textos
e a fonte pixel nas demonstrações.

As prévias do site são ilustrações animadas dos recursos. Não são capturas de
execução no Android. As licenças de fontes estão em `app/src/main/assets/licenses/`.
