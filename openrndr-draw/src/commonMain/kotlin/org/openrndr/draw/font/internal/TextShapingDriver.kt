package org.openrndr.draw.font.internal

import org.openrndr.draw.font.Face
import org.openrndr.math.Vector2
import kotlin.jvm.JvmRecord

/**
 * Represents the result of a shape operation, which is generally utilized in the rendering
 * or layout process of text or graphical shapes.
 *
 * @property glyphIndex The index of the glyph in the font. This typically references the visual
 * representation of a character or shape in the font's glyph table.
 * @property offset A 2D vector indicating the positional offset to apply to the glyph
 * during placement. This can be used to adjust its position for kerning or other layout needs.
 * @property advance A 2D vector representing the advance to apply after positioning the glyph.
 * This is used to determine the next position in the layout for rendering.
 */
@JvmRecord
data class ShapeResult(val glyphIndex: Int, val offset: Vector2, val advance: Vector2)


@JvmRecord
data class ShapeFeature(val tag: String, val value: Int, val range: UIntRange = UIntRange(0u, 0xff_ff_ff_ffu)) {

    companion object {
        val DISABLE_COMMON_LIGATURES = ShapeFeature("clig", 0)
        val ENABLE_SMALL_CAPS = ShapeFeature("smcp", 1)
        val ENABLE_ALL_SMALL_CAPS = ShapeFeature("c2sc", 1)
        val ENABLE_PETITE_CAPS = ShapeFeature("pcap", 1)
        val ENABLE_UNICASE = ShapeFeature("unic", 1)
        val ENABLE_TITLING_CAPS = ShapeFeature("titl", 1)
        val ENABLE_TABULAR_NUMERALS = ShapeFeature("tnum", 1)
    }


}

enum class Direction(val tag: String) {
    LEFT_TO_RIGHT("Ltr"),
    RIGHT_TO_LEFT("Rtl"),
    TOP_TO_BOTTOM("Ttb"),
    BOTTOM_TO_TOP("Btt"),
}

enum class Script(val tag: String) {
    COMMON("Zyyy"),
    INHERITED("Zinh"),
    UNKNOWN("Zzzz"),
    ARABIC("Arab"),
    ARMENIAN("Armn"),
    BENGALI("Beng"),
    CYRILLIC("Cyrl"),
    DEVANAGARI("Deva"),
    GEORGIAN("Geor"),
    GREEK("Grek"),
    GUJARATI("Gujr"),
    GURMUKHI("Guru"),
    HANGUL("Hang"),
    HAN("Hani"),
    HEBREW("Hebr"),
    HIRIGANA("Hira"),
    KANNADA("Knda"),
    KATAKANA("Kana"),
    LAOTHIAN("Laoo"),
    MALAYALAM("Mlym"),
    ORIYA("Orya"),
    TAMIL("Taml"),
    TELUGU("Telu"),
    THAI("Thai"),
    TIBETAN("Tibt"),
    BOPOMOFO("Bopo"),
    BRAILLE("Brai"),
    CANADIAN_SYLLABICS("Cans"),
    CHEROKEE("Cher"),
    ETHIOPIC("Ethi"),
    KHMER("Khmr"),
    MONGOLIAN("Mong"),
    MYANMAR("Mymr"),
    OGHAM("Ogam"),
    RUNIC("Runr"),
    SINHALA("Sinh"),
    SYRIAC("Syrc"),
    THAANA("Thaa"),
    YI("Yiii"),
    DESERET("Dsrt"),
    GOTHIC("Goth"),
    OLD_ITALIC("Ital"),
    BUHID("Buhd"),
    HANUNOO("Hano"),
    TAGALOG("Tglg"),
    TAGBANWA("Tagb"),
    CYPRIOT("Cprt"),
    LIMBU("Limb"),
    LINEAR_B("Linb"),
    OSMANYA("Osma"),
    SHAVIAN("Shaw"),
    TAI_LE("Tale"),
    UGARITIC("Ugar"),
    BUGINESE("Bugi"),
    COPTIC("Copt"),
    GLAGOLITIC("Glag"),
    KHAROSHTHI("Khar"),
    NEW_TAI_LUE("Talu"),
    OLD_PERSIAN("Xpeo"),
    SYLOTI_NAGRI("Sylo"),
    TIFINAGH("Tfng"),
    BALINESE("Bali"),
    CUNEIFORM("Xsux"),
    NKO("Nkoo"),
    PHAGS_PA("Phag"),
    PHOENICIAN("Phnx"),
    CARIAN("Cari"),
    CHAM("Cham"),
    KAYAH_LI("Kali"),
    LEPCHA("Lepc"),
    LYCIAN("Lyci"),
    LYDIAN("Lydi"),
    OL_CHIKI("Olck"),
    REJANG("Rjng"),
    SAURASHTRA("Saur"),
    SUNDANESE("Sund"),
    VAI("Vaii"),
    AVESTAN("Avst"),
    BAMUM("Bamu"),
    EGYPTIAN_HIEROGLYPHS("Egyp"),
    IMPERIAL_ARAMAIC("Armi"),
    INSCRIPTIONAL_PAHLAVI("Phli"),
    INSCRIPTIONAL_PARTHIAN("Prti"),
    JAVANESE("Java"),
    KAITHI("Kthi"),
    LISU("Lisu"),
    MEETEI_MAYEK("Mtei"),
    OLD_SOUTH_ARABIAN("Sarb"),
    OLD_TURKIC("Orkh"),
    SAMARITAN("Samr"),
    TAI_THAM("Lana"),
    TAI_VIET("Tavt"),
    BATAK("Batk"),
    BRAHMI("Brah"),
    MANDAIC("Mand"),
    CHAKMA("Cakm"),
    MEROITIC_CURSIVE("Merc"),
    MEROITIC_HIEROGLYPHS("Mero"),
    MIAO("Plrd"),
    SHARADA("Shrd"),
    SORA_SOMPENG("Sora"),
    TAKRI("Takr"),
    BASSA_VAH("Bass"),
    CAUCASIAN_ALBANIAN("Aghb"),
    DUPLOYAN("Dupl"),
    ELBASAN("Elba"),
    GRANTHA("Gran"),
    KHOJKI("Khoj"),
    KHUDAWADI("Sind"),
    LINEAR_A("Lina"),
    MAHAJANI("Mahj"),
    MANICHAEAN("Mani"),
    MENDE_KIKAKUI("Mend"),
    MODI("Modi"),
    MRO("Mroo"),
    NABATAEAN("Nbat"),
    OLD_NORTH_ARABIAN("Narb"),
    OLD_PERMIC("Perm"),
    PAHAWH_HMONG("Hmng"),
    PALMYRENE("Palm"),
    PAU_CIN_HAU("Pauc"),
    PSALTER_PAHLAVI("Phlp"),
    SIDDHAM("Sidd"),
    TIRHUTA("Tirh"),
    WARANG_CITI("Wara"),
    AHOM("Ahom"),
    ANATOLIAN_HIEROGLYPHS("Hluw"),
    HATRAN("Hatr"),
    MULTANI("Mult"),
    OLD_HUNGARIAN("Hung"),
    SIGNWRITING("Sgnw"),
    ADLAM("Adlm"),
    BHAIKSUKI("Bhks"),
    MARCHEN("Marc"),
    OSAGE("Osge"),
    TANGUT("Tang"),
    NEWA("Newa"),
    MASARAM_GONDI("Gonm"),
    NUSHU("Nshu"),
    SOYOMBO("Soyo"),
    ZANABAZAR_SQUARE("Zanb"),
    DOGRA("Dogr"),
    GUNJALA_GONDI("Gong"),
    HANIFI_ROHINGYA("Rohg"),
    MAKASAR("Maka"),
    MEDEFAIDRIN("Medf"),
    OLD_SOGDIAN("Sogo"),
    SOGDIAN("Sogd"),
    ELYMAIC("Elym"),
    NANDINAGARI("Nand"),
    NYIAKENG_PUACHUE_HMONG("Hmnp"),
    WANCHO("Wcho"),
    CHORASMIAN("Chrs"),
    DIVES_AKURU("Diak"),
    KHITAN_SMALL_SCRIPT("Kits"),
    YEZIDI("Yezi"),
    CYPRO_MINOAN("Cpmn"),
    OLD_UYGHUR("Ougr"),
    TANGSA("Tnsa"),
    TOTO("Toto"),
    VITHKUQI("Vith"),
    MATH("Zmth"),
    KAWI("Kawi"),
    NAG_MUNDARI("Nagm"),
    GARAY("Gara"),
    GURUNG_KHEMA("Gukh"),
    KIRAT_RAI("Krai"),
    OL_ONAL("Onao"),
    SUNUWAR("Sunu"),
    TODHRI("Todr"),
    TULU_TIGALARI("Tutg"),
    BERIA_ERFE("Berf"),
    SIDETIC("Sidt"),
    TAI_YO("Tayo"),
    TOLONG_SIKI("Tols"),
}

/**
 * Interface for implementing text shaping logic.
 *
 * A `TextShapingDriver` is responsible for converting a string of characters into a visually
 * accurate sequence of shaped glyphs, accounting for font-specific details, writing direction,
 * and script characteristics. This process is pivotal in rendering text in a way that adheres
 * to the typographic and linguistic rules of the specified script and language system.
 */
interface TextShapingDriver {

    /**
     * Shapes the given text into a sequence of glyphs according to the specific font face and optional
     * typographic attributes like writing direction and script.
     *
     * @param face the font face used for shaping the text. This determines the visual style
     *             and supported glyphs.
     * @param text the input string to be shaped into glyphs.
     * @param direction the optional writing direction for the text (e.g., left-to-right, right-to-left).
     *                  If null, a default direction may be used based on the script or context.
     * @param script the optional script used for shaping, specifying the writing system
     *               (e.g., Latin, Arabic, Chinese). If null, the script may be inferred from the text.
     * @param language the optional language system used for shaping, specified as BCP 47 language tag.
     * @return a list of shaped results, where each result includes information about the shaped
     *         glyphs, their positions, and any other relevant shaping data.
     */
    fun shape(
        face: Face,
        text: String,
        features: List<ShapeFeature> = emptyList(),
        direction: Direction? = null,
        script: Script? = null,
        language: String? = null,

    ): List<ShapeResult>

    /**
     * Queries the substitution features available in the specified font face for a given script.
     *
     * Substitution features define how characters or glyphs are replaced or altered in the text
     * shaping process for the specified script. This method returns a list of feature identifiers
     * that represent the supported substitution capabilities.
     *
     * @param face the font face to query. This specifies the source of typographic data and capabilities.
     * @param script the script for which substitution features are being queried. It determines
     *               the writing system context (e.g., Latin, Arabic, Chinese).
     * @return a list of strings, where each string is an identifier of a supported substitution feature.
     */
    fun querySubstitutionFeatures(face: Face, script: Script): List<String>

    /**
     * Queries the position features available in the specified font face for a given script.
     *
     * Position features define adjustments to glyph positioning during text shaping,
     * such as kerning and positioning relative to baseline or other glyphs. This method
     * returns a list of feature identifiers that represent the supported positioning
     * capabilities in the context of the specified script.
     *
     * @param face the font face to query. This specifies the source of typographic data
     *             and capabilities.
     * @param script the script for which position features are being queried. It determines
     *               the writing system context (e.g., Latin, Arabic, Chinese).
     * @return a list of strings, where each string is an identifier of a supported positioning feature.
     */
    fun queryPositionFeatures(face: Face, script: Script): List<String>

    /**
     * Queries the scripts supported by the specified font face.
     *
     * This function returns a list of scripts that the given font face can handle during text shaping.
     * Each script represents a writing system, such as Latin, Arabic, or Chinese, that the font face
     * supports for rendering text.
     *
     * @param face the font face for which the supported scripts are being queried. It determines the
     *             capabilities and supported writing systems of the font.
     * @return a list of supported [Script] objects, where each [Script] describes a writing system
     *         supported by the specified font face.
     */
    fun querySupportedScripts(face: Face): List<Script> {
        return emptyList()
    }

    companion object {
        var driver: TextShapingDriver? = null
        val instance: TextShapingDriver
            get() {
                return driver ?: error("TextShapingDriver not initialized")
            }
    }
}