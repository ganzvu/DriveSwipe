package com.example.driveswipe.ui.theme

import androidx.compose.ui.graphics.Color

// Main Color (Primary Teal from Stitch)
val MainBlue = Color(0xFF57F1DB) // #57F1DB

// Sub Colors
val SubBlueLight = Color(0xFFBACAC5)  // #BACAC5 (On Surface Variant)
val SubBlueBright = Color(0xFF3CDDC7) // #3CDDC7 (Surface Tint)
val SubBlueDark = Color(0xFF152031)   // #152031 (Surface Container)

// Semantic mapping to existing theme variables to maintain codebase compatibility
val DarkBg = Color(0xFF081425)         // #081425 (Background)
val DarkSurface = Color(0xFF111C2D)    // #111C2D (Surface Container Low)
val DarkCard = Color(0x991E293B)       // #1E293B with alpha for glass (rgba(30, 41, 59, 0.6))
val DarkBorder = Color(0xFF3C4A46)     // #3C4A46 (Outline Variant)

val AccentCyan = MainBlue              // #57F1DB
val AccentSteel = SubBlueBright        // #3CDDC7

val TextPrimary = Color(0xFFD8E3FB)    // #D8E3FB (On Surface)
val TextSecondary = SubBlueLight       // #BACAC5 (On Surface Variant)

val StateAlerting = Color(0xFFFF9100)  // Safety Amber
val StateActive = Color(0xFF57F1DB)    // Active State is Teal
val StateError = Color(0xFFFFB4AB)     // #FFB4AB (Error)



