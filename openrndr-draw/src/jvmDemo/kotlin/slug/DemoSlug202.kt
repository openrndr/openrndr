package slug

import FontDriverFreetype
import TextShapingDriverHarfBuzz
import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            FontDriver.driver = FontDriverFreetype()
            val pface = loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 32.0, 1.0)
            val m = pface.master
            m["Weight"] = 900.0
            val face = pface.withMaster(m)
            //val face = loadFace("data/fonts/default.otf", 12.0, 1.0)

            val slugMap = SlugMap(
                colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
                colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
            )


            val slugGlyphMap = SlugGlyphMap(slugMap)

            val texts = listOf(
                "형안안안안안녕하세요! 안녕하세요!",
                "こんにちは世界！",
                "你好世界！",
                "안안안안안안녕하세요! 안녕하세요!",
                "안안안안안안녕하세요! 안녕하세요!",
                "세상에, 저런 달팽이 상형",
                "HERE WE ARE NOWaaa",
                "ENTERTAIN US",
                "Hello, hello, hello, how low?\n" +
                        "Hello, hello, hello, how low?\n" +
                        "Hello, hello, hello, how low?\n" +
                        "Hello, hello, hello",
                "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE",
                "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHEHELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHEHELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE \"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HEHEHELLHEOHELHELHEOHELHELHE  LEHLEHE LHE",


            )


            val shaper = TextShapingDriverHarfBuzz()
            val shapeResults = texts.map { shaper.shape(face, it) }


            val glyphCount = shapeResults.sumOf { it.size }

            val shapeResultsAll = shapeResults

            for ((index, shapeResult) in shapeResultsAll.withIndex()) {
                for (item in shapeResult) {
                    slugGlyphMap.getSlugForGlyphIndex(face, item.glyphIndex)
                }
            }

            val vb = vertexBuffer(vertexFormat {
                position(3)
                textureCoordinate(2)
            }, 6)

            val b = Rectangle(0.0, 0.0, 1.0, 1.0)
            vb.put {
                write(b.position(0.0, 0.0).xy0)
                write(Vector2(0.0, 0.0))

                write(b.position(1.0, 0.0).xy0)
                write(Vector2(1.0, 0.0))

                write(b.position(1.0, 1.0).xy0)
                write(Vector2(1.0, 1.0))

                write(b.position(1.0, 1.0).xy0)
                write(Vector2(1.0, 1.0))

                write(b.position(0.0, 1.0).xy0)
                write(Vector2(0.0, 1.0))

                write(b.position(0.0, 0.0).xy0)
                write(Vector2(0.0, 0.0))
            }

            val instances = vertexBuffer(vertexFormat {
                attribute("transform", VertexElementType.MATRIX44_FLOAT32)
                attribute("slugBounds", VertexElementType.VECTOR4_FLOAT32)
                attribute("bandIndex", VertexElementType.VECTOR2_INT32)
                attribute("bandCount", VertexElementType.INT32)
            }, 10_000)

            instances.put {

                for ((index, shapeResult) in shapeResultsAll.withIndex()) {
                    var cursor = Vector2(0.0, (index + 1) * face.height)

                    for ((index, i) in shapeResult.withIndex()) {
                        val slugIndex = slugGlyphMap.getSlugForGlyphIndex(face, i.glyphIndex)
                        val slugBounds = slugMap.bounds[slugIndex]
                        val slugBandIndex = slugMap.bandIndices[slugIndex]
                        val slugBandCount = slugMap.bandCounts[slugIndex]

                        write(transform {
                            translate(cursor - i.offset )
                        })
                        write(
                            Vector4(
                                slugBounds.x,
                                slugBounds.y,
                                slugBounds.x + slugBounds.width,
                                slugBounds.y + slugBounds.height
                            )
                        )
                        write(slugBandIndex)
                        write(slugBandCount)

                        cursor += shapeResult[index].advance
                    }

                }
                slugMap.bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)

            }

            val slugPhrase = """
                float saturate(float x) {
                    return clamp(x, 0.0, 1.0);
                }
uint CalcRootCode(float y1, float y2, float y3)
{
	// Calculate the root eligibility code for a sample-relative quadratic Bézier curve.
	// Extract the signs of the y coordinates of the three control points.

	uint i1 = floatBitsToUint(y1) >> 31U;
	uint i2 = floatBitsToUint(y2) >> 30U;
	uint i3 = floatBitsToUint(y3) >> 29U;

	uint shift = (i2 & 2U) | (i1 & ~2U);
	shift = (i3 & 4U) | (shift & ~4U);

	// Eligibility is returned in bits 0 and 8.

	return ((0x2E74U >> shift) & 0x0101U);
}

vec2 SolveHorizPoly(vec4 p12, vec2 p3)
{
	// Solve for the values of t where the curve crosses y = 0.
	// The quadratic polynomial in t is given by
	//
	//     a t^2 - 2b t + c,
	//
	// where a = p1.y - 2 p2.y + p3.y, b = p1.y - p2.y, and c = p1.y.
	// The discriminant b^2 - ac is clamped to zero, and imaginary
	// roots are treated as a double root at the global minimum
	// where t = b / a.

	vec2 a = p12.xy - p12.zw * 2.0 + p3;
	vec2 b = p12.xy - p12.zw;
	float ra = 1.0 / a.y;
	float rb = 0.5 / b.y;

	float d = sqrt(max(b.y * b.y - a.y * p12.y, 0.0));
	float t1 = (b.y - d) * ra;
	float t2 = (b.y + d) * ra;

	// If the polynomial is nearly linear, then solve -2b t + c = 0.

	if (abs(a.y) < 1.0 / 65536.0) t1 = t2 = p12.y * rb;

	// Return the x coordinates where C(t) = 0.

	return (vec2((a.x * t1 - b.x * 2.0) * t1 + p12.x, (a.x * t2 - b.x * 2.0) * t2 + p12.x));
}

vec2 SolveVertPoly(vec4 p12, vec2 p3)
{
	// Solve for the values of t where the curve crosses x = 0.

	vec2 a = p12.xy - p12.zw * 2.0 + p3;
	vec2 b = p12.xy - p12.zw;
	float ra = 1.0 / a.x;
	float rb = 0.5 / b.x;

	float d = sqrt(max(b.x * b.x - a.x * p12.x, 0.0));
	float t1 = (b.x - d) * ra;
	float t2 = (b.x + d) * ra;

	// If the polynomial is nearly linear, then solve -2b t + c = 0.

	if (abs(a.x) < 1.0 / 65536.0) t1 = t2 = p12.x * rb;

	// Return the y coordinates where C(t) = 0.

	return (vec2((a.y * t1 - b.y * 2.0) * t1 + p12.y, (a.y * t2 - b.y * 2.0) * t2 + p12.y));
}                
                
float CalcCoverage(float xcov, float ycov, float xwgt, float ywgt, int flags)
{
	// Combine coverages from the horizontal and vertical rays using their weights.
	// Absolute values ensure that either winding direction convention works.

	float coverage = max(abs(xcov * xwgt + ycov * ywgt) / max(xwgt + ywgt, 1.0 / 65536.0), min(abs(xcov), abs(ycov)));

	// If SLUG_EVENODD is defined during compilation, then check E flag in tex.w. (See vertex shader.)

	#if defined(SLUG_EVENODD)

		if ((flags & 0x1000) == 0)
		{

	#endif

			// Using nonzero fill rule here.

			coverage = clamp(coverage, 0.0, 1.0);

	#if defined(SLUG_EVENODD)

		}
		else
		{
			// Using even-odd fill rule here.

			coverage = 1.0 - abs(1.0 - frac(coverage * 0.5) * 2.0);
		}

	#endif

	// If SLUG_WEIGHT is defined during compilation, then take a square root to boost optical weight.
	#if defined(SLUG_WEIGHT)
    coverage = sqrt(coverage);
	#endif
	return (coverage);
}                
            """.trimIndent()

            extend {
                drawer.translate(drawer.bounds.center)
                //drawer.scale(1.0 + mouse.position.y / height)
                drawer.translate(-drawer.bounds.center)
                drawer.shadeStyle = shadeStyle {
                    vertexTransform = """
                        x_position = vec3(mix(i_slugBounds.xy, i_slugBounds.zw, a_texCoord0.xy), 0.0);
                        x_position = (i_transform * vec4(x_position, 1.0)).xyz;
                    """

                    fragmentPreamble = """
                        ${slugPhrase}
                        vec4 flatFetch(sampler2D tex, int index) {
                            ivec2 index2d = ivec2(index % 4096, index / 4096);
                            return texelFetch(tex, index2d, 0);
                        }
                       uvec4 flatFetch(usampler2D tex, int index) {
                            ivec2 index2d = ivec2(index % 4096, index / 4096);
                            return texelFetch(tex, index2d, 0);
                        }
                        
                       
                        void readCurve(ivec2 curveIndex, out vec2 a, out vec2 b, out vec2 c) {
                            int flatIndex = curveIndex.x + curveIndex.y * 4096;
                            
                            a = flatFetch(p_curves, flatIndex).xy;
                            b = flatFetch(p_curves, flatIndex + 1).xy;
                            c = flatFetch(p_curves, flatIndex + 2).xy;
                        }
                        
                        void readBandHeader(ivec2 bandIndex, int bandId, out int curveCount, out int curveIndex, out int band) {
                        
                            int bandIndexFlat = bandIndex.y * 4096 + bandIndex.x + bandId;
                            ivec2 bandIndexFinal = ivec2(bandIndexFlat % 4096, bandIndexFlat / 4096);
                        
                            uvec4 data = texelFetch(p_bands, bandIndexFinal, 0);
                            curveCount = int(data.r);
                            curveIndex = int(data.g);
                            band = int(data.b);
                        }
                        
                        void readBandCurveIndex(int curveId, out ivec2 ascendingIndex, out ivec2 descendingIndex) {
                            int curveIndexFlat = vi_bandIndex.y * 4096 + vi_bandIndex.x + curveId + vi_bandCount * 2;
                            
                            uvec4 data = flatFetch(p_bands, curveIndexFlat);
                            
                            ascendingIndex = ivec2(int(data.r), int(data.g));
                            descendingIndex = ivec2(int(data.b), int(data.a));
                        }
                    """.trimIndent()

                    fragmentTransform = """
                        #define FILTER_SPREAD ((1.0 / 3.0))
                        #define FILTER_SAMPLES 5
                         vec2 slugPosition = mix(vi_slugBounds.xy, vi_slugBounds.zw, va_texCoord0.xy);
       
                        vec2 emsPerPixel = fwidth(slugPosition);
	                    vec2 pixelsPerEm = 1.0 / emsPerPixel;
                        
                        
                        ivec2 bandIndex = ivec2(floor(va_texCoord0.xy * float(vi_bandCount) ));
                        
                        int hCurveCount = 0;
                        int hCurveIndex = 0;
                        int hBand = 0;                        
                        readBandHeader(vi_bandIndex, bandIndex.y, hCurveCount, hCurveIndex, hBand);
                        
                        float xcov = 0.0;
                        float xwgt = 0.0;

                        float xcovs[FILTER_SAMPLES] = float[](0.0, 0.0, 0.0, 0.0, 0.0);
                        float xwgts[FILTER_SAMPLES] = float[](0.0, 0.0, 0.0, 0.0, 0.0);


                        for (int i = 0; i < hCurveCount; i++) {
                            ivec2 ascending = ivec2(0, 0);
                            ivec2 descending = ivec2(0, 0);
                            readBandCurveIndex(hCurveIndex + i, ascending, descending);
                            
                            vec2 a, b, c;
                            readCurve(ascending, a, b, c);
                            
                            
                            for (int s = 0; s < FILTER_SAMPLES; ++s) {
                            vec2 o = float(s-FILTER_SAMPLES/2) * vec2(0.0, emsPerPixel.y * FILTER_SPREAD);
                            
                            vec4 p12 = vec4(a, b) - vec4(slugPosition + o, slugPosition + o);
                            vec2 p3 = c - (slugPosition + o);
                            
                            uint code = CalcRootCode(p12.y, p12.w, p3.y);
                            if (code != 0U) {
                                // At least one root makes a contribution. Calculate them and scale so
                                // that the current pixel corresponds to the range [0,1].
                    
                                vec2 r = SolveHorizPoly(p12, p3) * pixelsPerEm.x;
                    
                                // Bits in code tell which roots make a contribution.
                    
                                if ((code & 1U) != 0U)
                                {
                                    xcovs[s] += clamp(r.x + 0.5, 0.0, 1.0);
                                    xwgts[s] = max(xwgts[s], clamp(1.0 - abs(r.x) * 2.0, 0.0, 1.0));
                                }
                    
                                if (code > 1U)
                                {
                                    xcovs[s] -= clamp(r.y + 0.5, 0.0, 1.0);
                                    xwgts[s] = max(xwgts[s], clamp(1.0 - abs(r.y) * 2.0, 0.0, 1.0));
                                }
		                    }
                            }
                        }
                        xcov = (xcovs[0] + xcovs[1] + xcovs[2] + xcovs[3] + xcovs[4]) / 5.0;
                        xwgt = (xwgts[0] + xwgts[1] + xwgts[2] + xwgts[3] + xwgts[4]) / 5.0;
                        
                        int vCurveCount = 0;
                        int vCurveIndex = 0;
                        int vBand = 0;     
                        float ycov = 0.0;
                        float ywgt = 0.0;
                                           
                        float ycovs[FILTER_SAMPLES] = float[](0.0, 0.0, 0.0, 0.0, 0.0);
                        float ywgts[FILTER_SAMPLES] = float[](0.0, 0.0, 0.0, 0.0, 0.0);
                                           
                        readBandHeader(vi_bandIndex, vi_bandCount + bandIndex.x, vCurveCount, vCurveIndex, vBand);

                        for (int i = 0; i < vCurveCount; i++) {
                            ivec2 ascending = ivec2(0, 0);
                            ivec2 descending = ivec2(0, 0);
                            readBandCurveIndex(vCurveIndex + i, ascending, descending);
                            
                            vec2 a, b, c;
                            readCurve(ascending, a, b, c);
                            
                            for (int s = 0; s < FILTER_SAMPLES; ++s) {
                            vec2 o = float(s-FILTER_SAMPLES/2) * vec2(emsPerPixel.x * FILTER_SPREAD, 0.0);
                            
                            vec4 p12 = vec4(a, b) - vec4(slugPosition + o, slugPosition + o);
                            vec2 p3 = c - (slugPosition + o);
                            
                            uint code = CalcRootCode(p12.x, p12.z, p3.x);
                            if (code != 0U) {
                                vec2 r = SolveVertPoly(p12, p3) * pixelsPerEm.y;

                                if ((code & 1U) != 0U) {
                                    ycovs[s] -= saturate(r.x + 0.5);
                                    ywgts[s] = max(ywgts[s], saturate(1.0 - abs(r.x) * 2.0));
                                }
                    
                                if (code > 1U) {
                                    ycovs[s] += saturate(r.y + 0.5);
                                    ywgts[s] = max(ywgts[s], saturate(1.0 - abs(r.y) * 2.0));
                                }
                            }
                        }
                        }
                        ycov = (ycovs[0] + ycovs[1] + ycovs[2] + ycovs[3] + ycovs[4]) / 5.0;
                        ywgt = (ywgts[0] + ywgts[1] + ywgts[2] + ywgts[3] + ywgts[4]) / 5.0;
                        float coverage = CalcCoverage(xcov, ycov, xwgt, ywgt, 0);
                        x_fill = vec4(1.0, 1.0, 1.0, coverage);
                    """
                    parameter("bands", slugMap.bands)
                    parameter("curves", slugMap.curves)
                }
                drawer.vertexBufferInstances(listOf(vb), listOf(instances), DrawPrimitive.TRIANGLES, glyphCount)
            }
        }
    }
}