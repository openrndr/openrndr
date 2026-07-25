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
import org.openrndr.draw.slug.SlugGlyphMap2
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.slug.SlugMap2
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
            val face = loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 40.0, 1.0)

            val slugMap = SlugMap2(
                colorBuffer(4096, 16, type = ColorType.FLOAT32, format = ColorFormat.RG),
                colorBuffer(4096, 16, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
            )

            for (i in face.axes) {
                println("$i ${face.getAxisValue(i)}")
            }
            face.setAxisValue("Weight", 100.0)

            val slugGlyphMap = SlugGlyphMap2(slugMap)

            val texts = listOf(
                "안녕하세요! 안녕하세요!",
                "안녕하세요! 안녕하세요!",
                "안녕하세요!",
                "안녕하세요! 안녕하세요! 안녕하세요!",
                "안녕하세요!",
                "안녕하세요!",
                "안녕하세요!",
                "안녕하세요!",
                "안녕하세요!",
                "안녕하세요!",
                "안녕하세요!",
                "세상에, 저런 달팽이 상형문자라니!"
            )

            val shaper = TextShapingDriverHarfBuzz()
            val shapeResults = texts.map { shaper.shape(face, it) }

            val glyphCount = shapeResults.sumOf { it.size }

            for ((index, shapeResult) in shapeResults.withIndex()) {
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

                for ((index, shapeResult) in shapeResults.withIndex()) {
                    var cursor = Vector2(0.0, (index + 1) * face.height)
                    face.setAxisValue("Weight", 100.0 + index * 100.0)

                    for ((index, i) in shapeResult.withIndex()) {
                        val slugIndex = slugGlyphMap.getSlugForGlyphIndex(face, i.glyphIndex)
                        val slugBounds = slugMap.bounds[slugIndex]
                        val slugBandIndex = slugMap.bandIndices[slugIndex]
                        val slugBandCount = slugMap.bandCounts[slugIndex]

                        write(transform {
                            translate(cursor + shapeResult[index].offset)
                        })
                        write(
                            Vector4(
                                slugBounds.x,
                                slugBounds.y,
                                slugBounds.x + slugBounds.width,
                                slugBounds.y + slugBounds.height
                            )
                        )
                        println("slugBandIndex: $slugBandIndex, slugBandCount: $slugBandCount")
                        write(slugBandIndex)
                        write(slugBandCount)

                        cursor += shapeResult[index].advance
                    }

                }
                slugMap.bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)

            }

            val slugPhrase = """
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
                drawer.scale(1.0 + mouse.position.y / height)
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
                        
                        vec2 slugPosition = mix(vi_slugBounds.xy, vi_slugBounds.zw, va_texCoord0.xy);
                        
                        ivec2 bandIndex = ivec2(floor(va_texCoord0.xy * float(vi_bandCount) ));
                        
//                        vec2 bandF = floor(va_texCoord0.xy * float(vi_bandCount) )  / float(vi_bandCount);
//                        x_fill = vec4(bandF, 0.0, 1.0);
                        
                        int hCurveCount = 0;
                        int hCurveIndex = 0;
                        int hBand = 0;
                        
                        readBandHeader(vi_bandIndex, bandIndex.y, hCurveCount, hCurveIndex, hBand);

                        float curveBand = 0.0;
                        
                        float minDistance = 1E10;
                        for (int i = 0; i < hCurveCount; i++) {
                            ivec2 ascending = ivec2(0, 0);
                            ivec2 descending = ivec2(0, 0);
                            readBandCurveIndex(hCurveIndex + i, ascending, descending);
                            curveBand = float(descending.x) / float(vi_bandCount); //float(abs(descending.x - bandIndex.y));
                            
                            vec2 a, b, c;
                            readCurve(ascending, a, b, c);
                            
                            float dist;
                            dist = distance(slugPosition, a);
                            if (dist < minDistance) {
                                minDistance = dist;
                            }
                            dist = distance(slugPosition, b);
                            if (dist < minDistance) {
                                minDistance = dist;
                            }
                            dist = distance(slugPosition, c);
                            if (dist < minDistance) {
                                minDistance = dist;
                            }
                            
                        }
                                                
                        x_fill.r = smoothstep(1.0, 0.0, minDistance); //float(hBand) / float(vi_bandCount);
                        x_fill.b = 0.0; //float(bandIndex.y) / float(vi_bandCount);
                        x_fill.g = 0.0; //curveBand;
                        
                    """
                    parameter("bands", slugMap.bands)
                    parameter("curves", slugMap.curves)
                }
                drawer.vertexBufferInstances(listOf(vb), listOf(instances), DrawPrimitive.TRIANGLES, glyphCount)
            }
        }
    }
}