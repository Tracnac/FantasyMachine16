
======================================================================
                    FANTASY MACHINE - CPU 16-BIT 
                        DOCUMENTATION TECHNIQUE
======================================================================

ARCHITECTURE GÉNÉRALE :
• CPU 16-bit avec 8 registres multi-usage (R0-R7)
• Mémoire totale : 128 KiB (2 banques de 64 KiB chacune)
• Écran monochrome 640×480 (1 bit/pixel) stocké en Bank1
• Encodage : BIG ENDIAN (octet de poids fort en premier)
• Commutation Bank0↔Bank1 via registre BANK_REG
• DMA pour transferts rapides Bank0→Bank1

======================================================================
1. PLAN MÉMOIRE - VUE D'ENSEMBLE
======================================================================

CONCEPT CLEF : Le CPU voit TOUJOURS un espace d'adressage de 64 KiB 
(0x0000–0xFFFF), mais le CONTENU dépend du registre BANK_REG :

• BANK_REG = 0 → CPU accède à Bank0 (code/données/pile)
• BANK_REG = 1 → CPU accède à Bank1 (frame buffer/vidéo)

EXCEPTION IMPORTANTE : Les registres I/O (0xFE00–0xFFFF) restent 
TOUJOURS mappés sur Bank0, même si BANK_REG=1.

======================================================================
                    MAPPAGE MÉMOIRE DÉTAILLÉ 
======================================================================

======================================================================
Bank0 (Physique 0x00000–0x0FFFF) – Code / Données / IO fixes
======================================================================
======================================================================
Adresse (logique)   Taille        Usage
======================================================================
-------------------------------------------------------------
0x0000 – 0xEB7F     58 KiB        Code utilisateur + données
0xEB80 – 0xEBFF     128 Bytes     Reserved
0xEC00 – 0xFBFF     4 KiB         Stack
0xFC00 – 0xFDFF     512 Bytes     Reserved
0xFE00 – 0xFFFF     512 Bytes     Registres I/O (Voir section 6 ci-dessous)

======================================================================
Bank1 (Physique 0x10000–0x1FFFF) – Vidéo / Frame buffer etc.
======================================================================
======================================================================
Adresse (logique)   Taille        Usage
======================================================================
0x0000 – 0x95FF     38.4 KiB      Frame buffer monochrome
                                  (640×480 @ 1 bit/pixel)
0x9600 – 0xFFFF     ~27 KiB       Zone vidéo libre :
                                  - double buffering
                                  - sprites / tiles
                                  - LUT trigonométriques
======================================================================

ZONES RESERVEES - NE PAS UTILISER!
=================================
- 0xEB80 - 0xEBFF (128 bytes) : Reserve systeme
- 0xFC00 - 0xFDFF (512 bytes) : Reserve systeme

FORMAT INSTRUCTIONS (16 bits)
============================
[OPCODE:5][SIZE:1][SRC_MODE:2][SRC_REG:3][DST_MODE:2][DST_REG:3]

MODES D'ADRESSAGE
================
00 = Registre direct (Rn)
01 = Registre indirect ([Rn]) 
10 = Adresse absolue ($addr)
11 = Immediat (valeur 0xXXXX, %0000000 0000)

REGISTRES CPU
============
R0-R7    : Registres generaux (16 bits)
SP       : Stack Pointer (init a 0xFBFF)  
PC       : Program Counter
FLAGS    : N,C,Z,V,X,I

REGISTRES I/O PRINCIPAUX
======================
0xFE00 : BANK_REG (0=Bank0, 1=Bank1)
0xFE01 : VIDEO_CTRL  
0xFE02 : VSYNC_STAT
0xFE04 : DMA_SRC (16 bits)
0xFE06 : DMA_DST (16 bits)
0xFE08 : DMA_LEN (16 bits)
0xFE0A : CPU_CTRL (8 bits)
0xFE0C : DMA_CTRL (8 bits)
0xFF0B : INT_CTRL (8 bits) - controle des interruptions

Notes :
- Le CPU voit toujours 0x0000–0xFFFF, mais contenu dépend de BANK_REG.
- Vecteurs d’interruptions + IO restent FIXE en BANK0.
- Pour que le CPU travail en Bank1, il faut passer BANK_REG=1,
  puis revenir BANK_REG=0 pour continuer le code normal.
  Attention il n'y a pas de stack en Bank1. (Il est possible d'exécuter du code mais aucune opération de pile n'est possible)
- Pour les transferts privilégier accès par blocs (CPU) ou via le DMA.
  Le changement fréquent de BANK_REG est à éviter.

=========================================
2. Format d’instruction proposé (16 bits)
=========================================

    bits: 15 ........... 0
        [opcode:5][size:1][src_mode:2][src_reg:3][dst_mode:2][dst_reg:3]

    • opcode : 5 bits → 32 instructions possibles.
    • size : 1 bit → 0 = byte (.B), 1 = word (.W).
    • src_mode : 2 bits (4 modes d’adressage).
    • src_reg : 3 bits (jusqu’à 8 registres). Quand le mode est immédiat (11) ou absolu (10), les champs `src_reg` / `dst_reg` sont forcés à 0 et ignorés.
    • dst_mode : 2 bits (4 modes d’adressage).
    • dst_reg :  3 bits (jusqu’à 8 registres).

    • 00 Registre direct Rn (n = 0..7)
    • 01 Registre indirect [Rn]
    • 10 Adresse absolue $1234 (imm16)
    • 11 Immédiat imm16 (imm16)

    Si un des modes = « immédiat » (11) ou « absolu » (10), l’instruction est suivie d'un mot d'extension (imm16).
    Si les deux opérandes requièrent une extension, l'ordre des mots est : [imm16_src:16][imm16_dest:16] (source puis destination).
    Si deux imm16 sont nécessaires, les mots suivent l'ordre source puis destination : [imm16_src][imm16_dest].
    Les champs src_reg / dst_reg doivent être mis à 0 quand le mode est 10 ou 11.
    Les mots d'extension sont encodés en BIG ENDIAN.

=================================================
3. Registres CPU (8 registres généraux + SP + PC)
=================================================

    R0..R7 = registres généraux BIG ENDIAN.
    SP = stack pointer (initialisé à 0xFBFF).
         La pile utilise des mots (16-bit) par défaut.
         Les opérations PUSH/POP sont définies en word.
         Les valeurs sur la pile sont stockées en BIG ENDIAN.
         Le CPU PANIC sur underflow/overflow de la pile.
    PC = program counter.
    FLAGS = registre de flags (N, C, Z, V, X, I).
    operandes = 2 opérandes (source, destination)
    taille = byte ".B", word ".W"

  3.1
	FLAGS = [I][X][V][Z][C][N]
         	 7  4  3  2  1  0

    N (Negative/Sign) - bit 0
    1 = résultat négatif (bit de poids fort = 1)
    Affecté par : ADD, SUB, CMP, AND, OR, XOR, etc.

    C (Carry) - bit 1
    1 = retenue/emprunt du bit de poids fort
    Affecté par : ADD, SUB, décalages, rotations

    Z (Zero) - bit 2
    1 = résultat = 0
    Affecté par : toutes opérations arithmétiques/logiques

    V (oVerflow) - bit 3
    1 = débordement arithmétique signé
    Affecté par : ADD, SUB, MUL

    X (eXtended) - bit 4
    Operations 32-bit, division par zéro, etc.
    Votre ajout pour MUL/DIV (signée) et MULU/DIVU non signée.

    I (Interrupt) - bit 7
    1 = Interruptions désactivées
    Affecté par : instructions de contrôle des interruptions

4. Registres spéciaux (IO fixes en Bank0)

  IO (0xFE00 – 0xFFFF) – IO :
    0xFE00 : BANK_REG   (0 = Bank0, 1 = Bank1)
    0xFE01 : VIDEO_CTRL
    0xFE02 : VSYNC_STAT
    0xFE04 : DMA_SRC (16 bits)
    0xFE06 : DMA_DST (16 bits)
    0xFE08 : DMA_LEN (16 bits)
    0xFE0A : CPU_CTRL (8 bits) - contrôle CPU (HLT/DBG/RST bits)
    0xFE0C : DMA_CTRL (8 bits)

    4.1 Registre BANK_REG (0xFE00)
        0 = Bank0 (code + données)
        1 = Bank1 (frame buffer + données vidéo)
    4.2 Registre VIDEO_CTRL (0xFE01)
        Bit Nom / Fonction Description
        0 DISPLAY_ENABLE 0 = écran noir, 1 = afficher frame buffer
        1 INVERT_COLORS 1 = inversion des pixels (blanc → noir)
        2 SHOW_CURSOR 1 = affiche curseur / sprite spécial
        3 TEST_PATTERN 1 = affiche un motif fixe pour debug
        4-7 Reserved / extensions futur usage (scroll, double-buffer, effets…)
    4.3 Registre VSYNC_STAT (0xFE02)
        Bit Nom / Fonction Description
        0 VSYNC_FLAG 1 = en période de VSync (rafraîchissement écran)
        1-7 Reserved / extensions futur usage
    4.4 Registres DMA (0xFE04, 0xFE06, 0xFE08)
        Permet de copier un bloc mémoire rapidement de Bank0 vers Bank1.
        • Écrire l'adresse source (logique dans Bank0, 16 bits) dans `DMA_SRC` (0xFE04).
        • Écrire l'adresse destination (logique dans Bank1, 16 bits) dans `DMA_DST` (0xFE06).
        • Le contrôleur DMA ajoute la base physique de Bank1 (0x10000) pour accéder physiquement à la destination.
        • Écrire la longueur en octets dans `DMA_LEN` (0xFE08), permet des transfert 8 bits (Donc DMA_LEN peut être pair ou impair).
        • Démarrer le transfert en écrivant 1 dans le bit `DMA_STRT` de `DMA_CTRL` (0xFF0A).
        • Attention le DMA ne peut pas être interrompu.

        Comportement précis :
        - L'écriture de `DMA_LEN = 0` est traitée comme un no-op (aucun transfert ne sera effectué avec DMA_STRT).
        - Au démarrage du transfert, le bit `DMA_BUSY` (bit 0 de `DMA_CTRL` à 0xFE0C) est mis à 1. Le transfert copie les octets de Bank0 vers Bank1.
        - A la fin du transfert, DMA_BUSY est remis à 0 et `DMA_LEN` est remis à 0.
        - Le transfert copie les octets un par un de Bank0 (adresse logique source + base physique 0x0000) vers Bank1 (adresse logique destination + base physique 0x10000).
        - Si DMA_LEN est impair, le dernier octet est copié normalement (pas de padding).
        - Exemple : DMA_SRC=0x1000, DMA_DST=0x2000, DMA_LEN=3 → copie 3 octets de Bank0:0x1000-0x1002 vers Bank1:0x12000-0x12002.
        - Attention : les adresses logiques sont toujours 16 Bits
        - Le CPU n'est pas bloqué pendant le transfert DMA et peut continuer à exécuter des instructions.
          Cependant, il est recommandé de ne pas accéder aux zones mémoire impliquées dans le transfert
          (source ou destination) tant que DMA_BUSY est actif pour éviter des comportements indéfinis.
        - Si une nouvelle commande DMA est initiée (écriture de DMA_STRT=1) alors que DMA_BUSY est déjà à 1,
          la nouvelle commande est ignorée et le transfert en cours continue jusqu'à son terme.

    6.5 Registre DMA_CTRL (0xFF0C)
        Bit 0: DMA_BUSY    (lecture seule) - 1 = transfert en cours, 0 = terminé
        Bit 1: DMA_STRT   (écriture seule) - écrire 1 pour démarrer un transfert.
        Bits 2-7: Reserved / extensions futur usage
        ATTENTION : Après implémentation des intérruptions, il sera nécessaire de désactiver les interruptions avant le transfert DMA.

    6.6 Registre CPU_CTRL (0xFE0A)

        Octet de contrôle côté CPU. Bits définis :
        Bit 0: HLT_FLAG - lecture : 1 = CPU halted, 0 = CPU running.
            Écriture : écrire 1 met le CPU en halt (le CPU cesse de fetch/exécuter des instructions).
            Écrire 0 reprend l'exécution (Mode debug seulement).
        Bit 1: DBG_FLAG - écriture 1 déclenche une interruption logicielle vers DEBUG_VECTOR (0xFFEA), entrant dans le debugger intégré.
               Écriture 0 sort du mode debug. Lecture retourne l'état (1 = en debug, 0 = normal).
        Bit 2: RST_FLAG - écriture 1 provoque un reset logiciel immédiat des registres et du contexte CPU : PC ← reset_vector, SP ← 0xFBFF, FLAGS cleared.
               IMPORTANT : le reset logiciel ne modifie PAS la mémoire (pas de nettoyage), ce qui permet de préserver un dump / state-machine pour debug.
               La lecture retourne 0.
        Bits 3-7: Reserved

        Notes :
        - L'écriture de `CPU_CTRL` est atomique sur l'octet ; écrire 0x05 (b00000101) mettra simultanément HLT=1 et RST=1.
        - Comportement HLT : lorsque HLT=1, le CPU arrête de fetcher et exécuter des instructions.
          Les périphériques (par ex. DMA) peuvent continuer selon implémentation matérielle. L'écriture de HLT=0 (par le même CPU via écriture mémoire IO ou via debug externe) reprend l'exécution.
        - Comportement RST : reset logiciel réinitialise registres et flags, repositionne PC et SP, mais ne touche pas à la RAM/ROM/Bank1 afin de permettre l'examen de l'état mémoire (dump) après reset.
        - Comportement DBG : écriture de 1 dans DBG_FLAG déclenche une interruption logicielle (sauvegarde PC/FLAGS sur la pile, désactive interruptions), puis saute à DEBUG_VECTOR (0xFFEA). Le debugger intégré permet inspection/modification des registres, mémoire, et step-by-step. Utiliser RETI pour sortir et reprendre l'exécution normale.

7. Plan d'implémentation des instructions - Fantasy CPU 16-bit
    Phase 1 : Instructions de base (8-10 opcodes)
    Objectif : CPU fonctionnel pour programmes simples

    Transfert de données (2 opcodes)

    MOV - Transfert source → destination

    Arithmétique de base (5 opcodes)

    ADD - Addition
    SUB - Soustraction
    INC - Incrément (+1)
    DEC - Décrément (-1)
    NEG - Négation (changement de signe)
    
    Contrôle de flux minimal (2 opcodes)

    JMP - Saut inconditionnel
    NOP - Pas d'opération

    Test de base (2 opcodes)

    CMP - Comparaison (soustraction sans stockage du résultat)
    TST - Test (AND logique sans stockage du résultat)

    Phase 2 : Extension logique et contrôle (6-8 opcodes)
    Objectif : Programmes avec boucles et conditions
    Opérations logiques (4 opcodes)

    AND - ET logique bit à bit
    OR - OU logique bit à bit
    XOR - OU exclusif bit à bit
    NOT - Complément logique

    Branchements conditionnels (maintenant centralisés)

    JCOND - Branchements conditionnels (voir spécification ci-dessous)

    l'instruction unique `JCOND` (opcode 0x1F) qui encode
    toutes les conditions usuelles. Les opcodes précédemment utilisés pour
    ces sauts sont réservés pour éviter les ambiguïtés.

    Phase 3 : Stack et sous-programmes (3 opcodes)
    Objectif : Appels de fonctions
    Gestion de pile (2 opcodes)

    PUSH - Empiler sur la stack
    POP - Dépiler de la stack

    Sous-programmes (2 opcodes)

    CALL - Appel de sous-programme
    RET - Retour de sous-programme
    RETI - Retour d'interruption

    Phase 4 : Arithmétique avancée (4-6 opcodes)
    Objectif : Calculs complexes
    Multiplication/Division (2-4 opcodes)

    MUL - Multiplication (signed)
    DIV - Division (signed)
    MULU - Multiplication (16x16→32 bits, résultat dans Rx:Rx, voir ci-dessous)
    DIVU - Division (voir ci-dessous, quotient → Rx, reste → Rx)

    Décalages (2 opcodes)

    SHL - Décalage logique à gauche
    SHR - Décalage logique à droite

    Rotations (2 opcodes)
    ROL - Rotation à gauche
    ROR - Rotation à droite
    
    Phase 5 : Manipulation de bits (3 opcodes)
    Objectif : Contrôle précis des registres I/O
    Opérations sur bits

    BTST - Test de bit
    BSET - Mise à 1 d'un bit
    BCLR - Mise à 0 d'un bit

    Phase 6 : (Reste 1 opcode)

    Total : 32 opcodes (0x00..0x1F)

    Priorités d'implémentation
    Critique (Phase 1)
        Sans ces instructions, impossible de faire un programme basique.
    Important (Phases 2-3)
        Nécessaire pour des programmes structurés avec boucles et fonctions.
    Utile (Phases 4-5)
        Améliore significativement les performances et facilite la programmation.
    Optionnel (Phase 6)
        Nice-to-have pour le debugging et la robustesse système.

    Considérations techniques
    Flags CPU affectés

    Z (Zero) : CMP, TST, arithmétique, logique
    C (Carry) : ADD, SUB, décalages
    S (Sign) : arithmétique, comparaisons
    V (Overflow) : ADD, SUB, MUL
    X (eXtended) : opérations 32-bit, division par zéro

    Instructions à extension

    Modes immédiat (imm) et absolu ($addr) nécessitent un ou deux mot(s) d'extension (imm16).
    BTST/BSET/BCLR peuvent prendre un numéro de bit immédiat.

    Cas spéciaux

    • MULU : résultat 32-bit (par convention : Rx = high 16 bits, Rx = low 16 bits).
      Convention : MULU src, dst signifie dst × src (multiplication de la destination par la source).
    • DIVU : quotient → Rx, reste → Rx (séparé pour signed/unsigned selon opcode).
      Convention : DIVU src, dst signifie dst ÷ src (division de la destination par la source).
    • JCOND : encodage spécial pour gérer toutes les conditions usuelles (voir ci-dessous).

    Table d'opcodes (valeurs 5 bits hex)
    0x00 NOP
    0x01 MOV
    0x02 ADD
    0x03 SUB
    0x04 INC
    0x05 DEC
    0x06 NEG
    0x07 JMP
    0x08 CMP
    0x09 TST
    0x0A AND
    0x0B OR
    0x0C XOR
    0x0D NOT
    0x0E RETI
    0x0F RESERVED
    0x10 MULU (unsigned)
    0x11 DIVU (unsigned)
    0x12 PUSH
    0x13 POP
    0x14 CALL
    0x15 RET
    0x16 MUL (signed)
    0x17 DIV (signed)
    0x18 ROL
    0x19 ROR
    0x1A SHL
    0x1B SHR
    0x1C BTST
    0x1D BSET
    0x1E BCLR
    0x1F JCOND

    L'opcode 0x1F est assigné à `JCOND` (voir spécification ci-dessous).
    Rappel : les mots d'extension (imm16 / adresse absolue) sont stockés en BIG ENDIAN et suivent immédiatement l'instruction (source puis destination si deux imm16).

7.1 Spécification détaillée de l'instruction RETI (opcode 0x0E)

    ------------------------------------------------------------------
    RETI — Retour d'interruption
    ------------------------------------------------------------------
    Objectif : Restaurer le contexte après traitement d'une interruption et reprendre l'exécution normale.

    Encodage (16-bit instruction) :
    - opcode = 0x0E (5 bits)
    - size = ignoré (pas d'opérande)
    - src_mode = 00 (registre direct, mais ignoré)
    - src_reg = 000 (ignoré)
    - dst_mode = 00 (registre direct, mais ignoré)
    - dst_reg = 000 (ignoré)
    - Pas d'extension.

    Comportement :
    1. Restaure FLAGS depuis la pile (POP FLAGS).
    2. Restaure PC depuis la pile (POP PC).
    3. Réactive les interruptions (bit I dans FLAGS remis à 0).
    4. Reprend l'exécution à l'adresse restaurée.

    Notes :
    - Utilisé uniquement dans les handlers d'interruptions.
    - Si la pile est corrompue, peut causer un CPU PANIC.
    - Diffère de RET : RET ne restaure pas FLAGS et ne réactive pas les interruptions.

7.2 Spécification détaillée de l'instruction JCOND (opcode 0x1F)

    ------------------------------------------------------------------
    JCOND — branche conditionnelle générale (opcode 0x1F)
    ------------------------------------------------------------------
    Objectif : fournir une instruction unique capable d'exprimer toutes les conditions usuelles
    (==, !=, >, <, >=, <=, signed/unsigned) en testant les flags CPU.

    Encodage (16-bit instruction) :
    - opcode = 0x1F (5 bits)
    - size bit : utilisé ici comme bit de poids fort du code de condition (cond_msb)
    - src_mode = 11 (imm) — indique que le champ `src_reg` contient le code de condition (bits bas)
    - src_reg (3 bits) : bits [2:0] du code de condition (cond_low3)
    - dst_mode = 10 (absolu) — indique qu'une extension imm16 suit (adresse cible)
    - dst_reg (3 bits) : ignoré


    Interprétation du code de condition (4 bits) :
    cond_code = (size << 3) | src_reg   ; // size = cond_msb

    Extensions :
    - un seul mot d'extension suit : `imm16_target` (adresse logique de saut, mappée selon BANK_REG rules).

    Table des cond_codes (hex / nom / condition) :
    0x0 : AL  (Always) - saut inconditionnel
    0x1 : EQ  (Z == 1)
    0x2 : NE  (Z == 0)
    0x3 : CS  (C == 1) / unsigned >=
    0x4 : CC  (C == 0) / unsigned <
    0x5 : MI  (N == 1)
    0x6 : PL  (N == 0)
    0x7 : VS  (V == 1)
    0x8 : VC  (V == 0)
    0x9 : GT  (signed >)  = (Z == 0) AND (N == V)
    0xA : GE  (signed >=) = (N == V)
    0xB : LT  (signed <)  = (N != V)
    0xC : LE  (signed <=) = (Z == 1) OR (N != V)
    0xD : HI  (unsigned >) = (C == 0) AND (Z == 0)
    0xE : HS  (unsigned >=) = (C == 0)
    0xF : LO  (unsigned <=) = (C == 1) OR (Z == 1)

    Avertissement : Pour JCOND, le bit `size` n'indique pas la taille d'opérande (byte/word) comme dans les autres instructions.
                    Il sert uniquement à étendre le code de condition à 4 bits. Ne pas confondre avec les opérations arithmétiques/logiques.

    Exemple d'encodage binaire complet pour JCOND EQ, $1234 :
    - Mot d'instruction (16 bits) : [0x1F (opcode)][0 (size=cond_msb)][11 (src_mode=imm)][001 (src_reg=cond_low3=1)][10 (dst_mode=abs)][000 (dst_reg=ignoré)] → 0xF9C0 (en hex, BIG ENDIAN).
    - Mot d'extension : 0x1234 (adresse cible).
    - Résultat : saut à 0x1234 si Z==1.
    
    Exemple d'encodage binaire complet pour JCOND GT, $2000 :

    - JCOND GT, $2000  (saut si A > B en signed ; après CMP A,B)
        cond_code 0x9 = 1001b -> cond_msb=1, cond_low3=001
        donc size bit devra être 1 et src_reg=0b001 ; assembler générera l'instruction correspondante et l'extension target.

    Remarque : cet encodage réutilise le bit `size` comme bit de condition (cond_msb) pour permettre 4 bits de condition sans mot d'extension additionnel. C'est un compromis volontaire : la signification du bit `size` change uniquement pour l'opcode `JCOND`.

    Extension future : si vous préférez conserver le sens de `size` strictement pour largeur, on peut alternativement encoder `cond_code` dans une extension `imm16_cond` et utiliser `dst_mode=10` + deux extensions [imm16_cond][imm16_target]. Cela coûte un mot additionnel mais donne plus de flexibilité (flags invert, masks, etc.).

8. Notes sur l'utilisation de

    BANK_REG et accès mémoire :
    - `BANK_REG` (IO 0xFF00) sélectionne la banque mappée sur l'espace logique 0x0000..0xFEFF. L'espace Stack, IO, etc (0xFE00..0xFFFF) reste fixe en Bank0.
    - Autrement dit, écrire `BANK_REG=1` fait apparaître le contenu de Bank1 sur les adresses 0x0000..0xFFFF (par ex. frame buffer)
    - ATTENTION :
            * la pile (SP) est située dans la zone logique 0xEC00..0xFBFF (physiquement Bank0).
            * Si `BANK_REG=1` est actif et que le code effectue des accès à la pile (PUSH/POP, JCOND, RET),
              ces accès toucheront Bank1 et provoqueront un état invalide ou indéfini.
            * Toujours revenir sur Bank0 (`BANK_REG=0`) avant d'utiliser la pile ou d'accéder aux IO.
            * Les accès rapides au frame buffer doivent être effectués avec précaution : préférer le DMA quand possible pour éviter de devoir switcher fréquemment.

9. Gestion des erreurs et interruptions

- CPU PANIC : Sur underflow (SP < 0xEC00) ou overflow (SP > 0xFBFF) de la pile, le CPU s'arrête immédiatement (HLT_FLAG=1). La mémoire reste intacte pour debug.
  Attention ceci est en cours de développement rien n'est encore acté, car cela empêche certain test CPU d'être effectué.
- Interruptions : Vecteurs fixes en Bank0 (ex. : reset à 0x0000, interruption à 0x0004). Non détaillées ici ; implémentation effectué, mais rien de défini.
- Flag X (eXtended) : Mis à 1 sur débordement 32-bit (MUL), division par zéro (DIV), ou erreurs arithmétiques. 

10. Vecteurs d'interruptions

Les interruptions permettent au CPU de réagir à des événements asynchrones (hardware ou software). Les vecteurs sont des adresses fixes en Bank0 où le CPU saute automatiquement lors d'une interruption.

- Table des vecteurs (adresses 16-bit, stockées en BIG ENDIAN) :
  Adresse   Nom / Description
  0xFFE0   RESET_VECTOR : Adresse de demarrage apres reset (hardware ou logiciel). Pointe vers le code d'initialisation.
  0xFFE2   IRQ_VECTOR   : Interrupt Request (generique, ex. : timer, I/O). Peut etre masquee via un registre.
  0xFFE4   NMI_VECTOR   : Non-Maskable Interrupt (priorite haute, ex. : erreur critique). Ne peut pas etre masquee.
  0xFFE6   BRK_VECTOR   : Point d'arret/Exception (breakpoint, software interrupt).
  0xFFE8   VSYNC_VECTOR : Interruption VSync (rafraichissement ecran).
  0xFFEA   DMA_VECTOR   : Interruption DMA (fin de transfert, erreur).
  0xFFEC   TIMER_VECTOR : Interruption timer (horloge interne).
  0xFFEE   USER0_VECTOR : Interruption utilisateur 0.
  0xFFF0   USER1_VECTOR : Interruption utilisateur 1.
  0xFFF2   USER2_VECTOR : Interruption utilisateur 2.
  0xFFF4   TRAP_VECTOR  : Instruction trap (piege logiciel).
  0xFFF6   DIV0_VECTOR  : Division par zero.
  0xFFF8   ILL_VECTOR   : Instruction illegale.
  0xFFFA   PRIV_VECTOR  : Violation privilege.
  0xFFFC   OVF_VECTOR   : Debordement arithmetique.

- Comportement lors d'une interruption :
  1. Le CPU sauvegarde PC et FLAGS sur la pile (PUSH PC, PUSH FLAGS).
  2. Désactive les interruptions (bit I dans FLAGS mis à 1).
  3. Lit l'adresse du vecteur et saute (PC ← [vecteur]).
  4. Le handler exécute le code approprié.
  5. RETI (opcode 0x0E) restaure FLAGS et PC depuis la pile, et réactive les interruptions.

- Registre associé : INT_CTRL (0xFF0B, 8 bits) pour gestion des interruptions.

    10.1 Détails du registre INT_CTRL (0xFF0B)

    Le registre INT_CTRL contrôle le masquage et le statut des interruptions. Il est accessible en lecture/écriture.

    Bits définis :
    - Bits 0-3 : Masque d'interruptions (Interrupt Mask, IM)
      * Bit 0 : IM_IRQ (1 = IRQ activée, 0 = masquée)
      * Bit 1 : IM_DMA (1 = DMA activée, 0 = masquée)
      * Bit 2 : IM_VSYNC (1 = VSYNC activée, 0 = masquée)
      * Bit 3 : IM_NMI (toujours 1, NMI non masquable)
    - Bits 4-7 : Statut des interruptions pendantes (Interrupt Status, IS)
      * Bit 4 : IS_IRQ (1 = IRQ pendante)
      * Bit 5 : IS_DMA (1 = DMA pendante)
      * Bit 6 : IS_VSYNC (1 = VSYNC pendante)
      * Bit 7 : IS_NMI (1 = NMI pendante)

    Comportement :
    - Une interruption est déclenchée seulement si son bit IM est à 1 et qu'elle survient.
    - Écrire dans IM permet de masquer/démasquer des interruptions (ex. : pendant un handler critique).
    - Les bits IS sont mis à 1 automatiquement quand une interruption est détectée ; ils sont remis à 0 après traitement (via RETI ou écriture manuelle).
    - Exemple : Pour masquer l'IRQ pendant un transfert DMA, écrire 0xFD (b11111101) pour IM_IRQ=0, puis remettre à 0xFF après.

    Notes : Ce registre est atomique ; les interruptions sont vérifiées à chaque cycle CPU.

    Notes :
    - Les interruptions sont déclenchées par des signaux externes ou internes (ex. : DMA finissant).
    - Priorité : NMI > IRQ > autres. Si plusieurs, traiter dans l'ordre.
    - Pour debug : Le mode DBG_FLAG peut logger les interruptions.

4. Exemple d’instructions

    Programme simple : Charger une valeur immédiate dans R0, puis sauter à une adresse si égal à zéro.

    Code assembleur :
    START:  MOV.W 0x0000, R0    ; Charge 0 dans R0 (word)
            CMP.W R0, 0x0000    ; Compare R0 avec 0
            JCOND EQ, LOOP        ; Si égal (Z=1), saute à LOOP
    LOOP:   NOP                   ; Boucle vide
            JMP LOOP

    Encodage binaire (BIG ENDIAN, mots 16 bits) :
    - MOV.W #0x0001, R0 : 0x0F00 0x0001 (opcode=0x01, size=1, src_mode=11, src_reg=0, dst_mode=00, dst_reg=0, extension=0x0001)
    - CMP.W R0, #0x0000 : 0x89F0 0x0000
    - JCOND EQ, LOOP : 0xF9C0 0x0008 (adresse relative simplifiée ; en pratique, utiliser des labels)
    - Etc.

    Note : Cet exemple montre l'usage des modes immédiat et absolu. Pour des programmes plus complexes, utiliser un assembleur.


11.0 Assembleur (En développement)

    Directives autorisées :
        .ORG $XXXX                              ; Définit l'adresse de départ du code (par défaut 0x0000)
        .START                                  ; Définit l'adresse de départ et initialise le PC

        LABEL:                                  ; Définit une étiquette (label) pour les sauts
            INSTRUCTION                         ; Instruction assembleur (ex. MOV.W R0, R1)
                .B                              ; Suffixe pour byte (8 bits)
                .W                              ; Suffixe pour word (16 bits)
                    0xXXXX                      ; Valeur hexadécimale (16 bits)
                    %XXXXXXXX                   ; Valeur binaire (8 bits)
                    XXXX                        ; Valeur décimale (16 bits)
                    $XXXX                       ; Adresse absolue (16 bits)

        .DATA                                   ; Début de la section données

        MYBYTES: .BYTES 0xXX, 0xYY, %XXXXXXXX   ; Déclare des octets (byte data)
        MYWORDS: .WORDS 0xXXXX, 0xYYYY          ; Déclare des mots (word data)
        MYASCII: .ASCII "texte"                 ; Déclare une chaîne ASCII (sans le null terminator)

        // .EQU LABEL, value                    ; Définit une constante (Non implémenté ici)

        .END                                   ; Fin du fichier source

    Format de la footer map (spécification de la table des sections)
    
    Il y a une gestion des chunks de données à la fin du fichier source.
    Le fichier binaire compilé est suivi d'une table de sections (footer map) qui décrit les blocs de données à charger en mémoire.
    Ce qui réduit considérablement la taille du binaire final en évitant les zones vides.
    Chaque entrée de la footer map décrit un bloc de données avec son adresse de départ, son type et sa longueur.
        
    La footer map utilise un format spécifique de 4 octets par entrée :

    [16bits start_address][15..14 type][13..0 length]
    Types d'entrées :

    00 = .byte (données octets)
    01 = .word (données mots 16-bit)
    10 = .ascii (chaînes de caractères)
    11 = .org (marqueurs d'organisationi mémoire avec gestion des chunks), longueur=0)

Glossaire :
- BIG ENDIAN : Octets stockés du plus significatif au moins (ex. : 0x1234 = 0x12 puis 0x34 en mémoire).
- Flags CPU : Indicateurs d'état (Z=zero, C=carry, etc.) mis à jour par les instructions.
- Mode d'adressage : Façon d'accéder à la mémoire, ou aux registres (registre direct, indirect, immédiat, absolu).
- Convention : Rx:Rx pour résultats 32-bit (Rx = high, Rx = low) (Rx = Quotien, Rx = Reste).
- Convention pour les operandes 0xXXXX Hexadécimal, %XXXXXXXX binaire, XXXX immédiat, $XXXX adresse absolue.


JAVA:

📁 Tests créés :
1. CPUOpcodesIndirectTest.java - Mode indirect [Rn]
  Tests pour tous les opcodes avec adressage indirect
  Couvre les cas : [R1] → R2, R1 → [R2], [R1] → [R2]
  Tests de validation des pointeurs et gestion banking
  Tests de flags avec opérations indirectes

2. CPUOpcodesAbsoluteTest.java - Mode absolu $addr
  Tests pour tous les opcodes avec adresses absolues
  Couvre : $2000 → R1, R1 → $2000, $2000 → $3000
  Tests spéciaux pour registres I/O (VIDEO_CTRL, BANK_REG, etc.)
  Tests avec banking et boundaries mémoire
  Tests avec plusieurs mots d'extension

3. CPUOpcodesImmediateTest.java - Mode immédiat #imm
  Tests pour tous les opcodes avec valeurs immédiates (source uniquement)
  Couvre : #1234 → R1, #5678 → [R1], #ABCD → $3000
  Tests des constantes communes (0x0000, 0xFFFF, 0x8000, etc.)
  Tests de flags et edge cases avec immédiats

4. CPUOpcodesMixedTest.java - Combinaisons de modes
  Tests complexes combinant différents modes
  Chaînes de transfert de données complexes
  Tests de performance et edge cases
  Auto-modification de code
  Copie mémoire-à-mémoire efficace
  Tests de banking avec modes mixtes

🎯 Couverture complète :
  32 opcodes testés avec tous leurs modes d'adressage supportés
  Tous les cas arithmétiques : ADD, SUB, INC, DEC, NEG, MUL, DIV
  Tous les cas logiques : AND, OR, XOR, NOT
  Comparaisons : CMP, TST
  Manipulation de bits : BTST, BSET, BCLR
  Pile : PUSH, POP
  Sauts : JMP, CALL
  Décalages : ROL, ROR, SHL, SHR
  Gestion des flags dans tous les contextes
  Banking et registres I/O
  Edge cases et conditions d'erreur
