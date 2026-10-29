<div align="center">
<img src="docs/assets/logo.png" width="280" alt="Anão de Firma" />

# Anão de Firma

**A firma não para. Você também não.**

Um arcade Android em pixel art dentro de uma fábrica de automóveis.

[**Baixar para Android**](https://github.com/walmirramos/Anao/raw/refs/heads/main/docs/downloads/AnaoDeFirma-1.6.apk)

Android 5.0+ · Português · Dubbox 2026
</div>

## Entre na linha de montagem

Um novo protagonista, uma nova identidade e seis setores industriais: estampagem, soldagem, pintura, montagem, qualidade e expedição. Carrocerias, robôs de solda, esteiras e estruturas metálicas compõem a fábrica inspirada na Stellantis.

Desvie de dutos, colunas e máquinas com faixas de segurança. Some peças, supere seu recorde e aproveite os poderes temporários.

## Ouça a fábrica

Trilha chiptune industrial original, motores, transportadores, sinais eletrônicos e impactos metálicos. Música e máquinas têm controles de volume separados.

## Como jogar

Toque na tela para impulsionar o personagem. Evite os obstáculos e o chão. O pneu dá uma vida extra contra canos, sem prazo; a parafusadeira dura dez segundos. A bazuca oferece três tiros automáticos, sem prazo para expirar. Nas configurações, personalize a experiência e as cores do personagem.

## Instalação

Baixe o APK acima e abra no Android. Autorize a instalação pelo aplicativo usado no download quando solicitado. Esta edição tem identificador próprio e pode ser instalada junto com outros jogos.

## Desenvolvimento

Projeto Android Java. JDK 17 ou 21, Android SDK 35.

```powershell
./gradlew.bat :app:assembleDebug
```

- `app/`: jogo, arte e áudio.
- `docs/`: apresentação e APK.
- `scripts/`: geração do áudio e verificações.
- [Identidade e sons](guides/INDUSTRIAL.md).
- [Licenças](LICENSE.md).

![Montadora em pixel art 16 bits](docs/assets/factory-16bit.png)

## A experiência

- Protagonista original e carros detalhados em pixel art 16 bits.
- Pneu com escudo de uma vida extra, sem tempo; parafusadeira por dez segundos; bazuca com três disparos sem cronômetro.
- Splash de moedas ao pontuar, com textos de bônus e ganhos no topo.
- Aos 20 coins, carteira de trabalho: +10 coins e coroa pelo restante da partida.
- Aos 30, o corpo dos canos se move por dentro de bordas e abertura fixas.
- Aos 40, iluminação macabra e trilha original de terror.
- Medalhas com gesto irreverente em pixel art. Abertura e loading exibem apenas logo e textos.

Detalhes de cada versão no [histórico](CHANGELOG.md).

A versão 1.4.2 corrige o [filtro que deixava os canos brancos](guides/ANDROID-17.md), preservando as texturas e a aceleração gráfica.
