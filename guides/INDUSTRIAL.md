# Anão de Firma — identidade industrial

Protagonista e logo preparados das referências fornecidas, usando a ferramenta integrada de imagens com transparência. Cenários e obstáculos desenhados no Canvas, em resolução nativa, sem filtro: estampagem, solda, pintura, montagem, qualidade e expedição.

Prompt do personagem: extrair somente a cabeça da segunda referência, preservar identidade, pose, traços e pixel art, com transparência real.

Prompt da logo: extrair o emblema Anão de Firma da primeira referência; preservar retrato, círculo vermelho, contorno branco, texto e sombras internas; remover apenas o fundo externo.

Áudio original sintetizado: quatro compassos a 120 BPM, baixo, percussão metálica, motores e transportadores. Impactos substituem tiros; máquinas substituem sirene. Recursos WAV mono PCM 16-bit, 22050 Hz, sem saturação. Música e máquinas têm controles independentes e pausam em segundo plano.

Aplicativo independente: com.walmirramos.anaodefirma. Os poderes e controles da cópia base foram preservados.

## Arquivos finais

- `app/src/main/res/drawable-nodpi/nengue_character.png`: protagonista transparente.
- `app/src/main/res/drawable-nodpi/corre_logo.png`: logo transparente.
- `WorldArt.java`: fábrica, obstáculos e piso em Canvas.
- `scripts/generate_industrial_audio.py`: síntese reproduzível (Python + NumPy).

## Prompts enviados à ferramenta integrada

Personagem: Prepare game production asset from the supplied second reference image (bald pixel art man's head, facing right). Extract ONLY that exact head as a standalone sprite with genuinely transparent background, preserving his identity, pose, facial features, pixel-art edges and colors. No letters, no badge, no body, no invented accessories. Tight square framing with a little transparent padding. First reference is branding context only. Save the image asset.

Logo: Extract the supplied Anão de Firma logo/badge unchanged as a production game title logo. Preserve exact bald character portrait, red circular badge, white pixel outline, and text exactly 'Anão de Firma'. Remove only outside solid black background to genuine transparency; preserve internal black outlines and shadows. No added factory, no extra lettering. Tight framing with transparent margin. The reference image is the edit target.
