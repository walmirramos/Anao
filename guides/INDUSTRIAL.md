# Anão de Firma — identidade industrial

Protagonista e logo preparados das referências fornecidas, usando a ferramenta integrada de imagens com transparência. Cenários e obstáculos desenhados no Canvas, em resolução nativa, sem filtro: estampagem, solda, pintura, montagem, qualidade e expedição.

Prompt do personagem: extrair somente a cabeça da segunda referência, preservar identidade, pose, traços e pixel art, com transparência real.

Prompt da logo: extrair o emblema Anão de Firma da primeira referência; preservar retrato, círculo vermelho, contorno branco, texto e sombras internas; remover apenas o fundo externo.

Áudio original sintetizado: groove mecânico a 100 BPM, motores, ar comprimido, ferramentas servo e transportadores. Impactos substituem tiros; máquinas substituem sirene. Recursos WAV mono PCM 16-bit, 22050 Hz, sem saturação. Música e máquinas têm controles independentes e pausam em segundo plano.

Aplicativo independente: com.walmirramos.anaodefirma. Os controles foram preservados; pneu e parafusadeira substituem banana e celular, e a bazuca mantém explosões destrutivas por dez segundos.

## Arquivos finais

- `app/src/main/res/drawable-nodpi/nengue_character.png`: protagonista transparente.
- `app/src/main/res/drawable-nodpi/corre_logo.png`: logo transparente.
- `WorldArt.java`: fábrica, obstáculos e piso em Canvas.
- `scripts/generate_industrial_audio.py`: síntese reproduzível (Python + NumPy).

## Prompts enviados à ferramenta integrada

Personagem: Prepare game production asset from the supplied second reference image (bald pixel art man's head, facing right). Extract ONLY that exact head as a standalone sprite with genuinely transparent background, preserving his identity, pose, facial features, pixel-art edges and colors. No letters, no badge, no body, no invented accessories. Tight square framing with a little transparent padding. First reference is branding context only. Save the image asset.

Logo: Extract the supplied Anão de Firma logo/badge unchanged as a production game title logo. Preserve exact bald character portrait, red circular badge, white pixel outline, and text exactly 'Anão de Firma'. Remove only outside solid black background to genuine transparency; preserve internal black outlines and shadows. No added factory, no extra lettering. Tight framing with transparent margin. The reference image is the edit target.

## Fundo 16 bits — versão 1.1

Ferramenta integrada de imagens. Resultado salvo em `app/src/main/res/drawable-nodpi/factory_16bit.png`, compartilhado por abertura, carregamento e jogo. Renderização nativa 288×512 sem filtragem. Logo ilustrada restrita às telas iniciais.

Prompt: Game production background for a portrait side-scrolling arcade set INSIDE a Brazilian automotive assembly factory inspired by Stellantis. Authentic detailed 16-bit SNES pixel art, crisp deliberate square pixel clusters, limited harmonious steel blue/teal palette with warm amber safety lights and orange robot arms. Portrait composition 3:4 aspect ratio, full bleed. Distant sawtooth industrial roof, skylights, steel trusses, suspended ducts and cable trays upper third, assembly robots at middle distance, several recognizable unfinished passenger car bodies on conveyor line near bottom quarter, reflective steel panels, yellow-black safety edges. Quiet central play area with clear low-contrast negative space, all machinery clearly in the BACKGROUND, no foreground obstacles or pipes, no protagonist or people, no HUD, no words or logos or watermarks. Side-on elevation, layered parallax-like depth, beautifully crafted game art, no photorealism, no blur, no gradients, no smooth 3D rendering. The bottom 10% is dark workshop foundation and rails. Background only, to be overlaid with animated car sprites and gameplay industrial obstacles.

Áudio 1.1: motores, ventilação, servo de ferramentas, ar pneumático e esteira; trilha mecânica de 100 BPM. Fail de trombone cômico sintetizado original, sem amostra de meme. Síntese reproduzível no script de áudio. O jogo não usa gravações reais de uma fábrica.
