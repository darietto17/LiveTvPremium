# LiveTvPremium

LiveTvPremium è un ecosistema completo per lo streaming IPTV premium, progettato nativamente per Android (Mobile & TV) e affiancato da un potente motore Python per la generazione e la classificazione dinamica delle liste M3U.

## 🚀 Architettura del Progetto

Il progetto è diviso in due macro-componenti interconnessi:

### 1. Generatore Python (`/scripts`)
Uno script automatizzato che scarica, unisce, riordina e pulisce le playlist M3U grezze.
- **Parsing Intelligente**: Estrae i canali da molteplici fonti internazionali.
- **Supporto EPG**: Integra nativamente la guida TV XMLTV.
- **Classificazione Dinamica (`categories.json`)**: Il cuore del sistema. Permette di definire macro-categorie personalizzate (es. `Sport`, `Intrattenimento`, `Cinema`) e assegnarle ai canali in base a keyword testuali, ripulendo i tag `group-title` sporchi delle IPTV grezze.

### 2. App Android (`/app`)
Un client nativo all'avanguardia scritto in **Kotlin** e **Jetpack Compose** che scarica le liste M3U direttamente da questa repository GitHub.
- **Interfaccia Netflix-Style**: Dashboard orizzontali fluide, Dark Theme e layout a schede per Live TV, Film e Serie.
- **Ordinamento Dinamico**: Il tab "Live TV" legge direttamente `categories.json` scaricato da GitHub per ordinare visivamente le categorie esattamente nella frequenza e sequenzialità definita dall'utente.
- **Integrazione TMDB API**: Arricchisce on-the-fly Film e Serie scaricando locandine in Alta Definizione, trame, rating e Trailer YouTube ufficiali.
- **Player Premium**: Sfrutta `Media3` (ExoPlayer) ottimizzato con supporto PiP (Picture-in-Picture), selezione tracce Audio, Sottotitoli e tracking della Cronologia ("Continua a guardare" per il VOD).
- **Elusione Blocchi Regionali**: Riconoscimento intelligente del traffico VOD e reindirizzamento trasparente dei manifest video (es. `m3u8` di Film/Serie) tramite proxy dedicato per bypassare i blocchi ISP nativamente dal client Android.

## 🛠️ Come Generare la Playlist
Qualsiasi modifica apportata su `categories.json` o richieste di refresh dei link IPTV necessitano dell'esecuzione del compilatore Python:

```bash
# Entra nella cartella di test
cd scripts

# (Opzionale) Installa i requisiti per Playwright se è la prima volta
pip install -r requirements.txt
playwright install chromium

# Lancia il builder
python lista.py
```
A fine esecuzione troverai i file `.m3u` formattati, classificati e pronti al commit nella root del progetto. 
Basterà spingere la repo su GitHub (`git push`) e l'App Android rileverà automaticamente i cambiamenti forzando il ridownload al prossimo avvio o premendo il tasto "Sincronizza".

## 📱 Build Android App
Per compilare l'APK:
1. Apri la root del progetto in **Android Studio**.
2. Sincronizza Gradle.
3. [Opzionale] Inserisci in Gradle/Datastore una tua chiave TMDB e il Token GitHub se il repo è privato.
4. Premi **Run / Build APK**.

---
*Progetto a scopo di studio sull'interoperabilità tra Parser Testuali Python remoti e UI Compose reattive.*
