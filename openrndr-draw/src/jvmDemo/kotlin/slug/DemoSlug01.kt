package slug

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.slug.SlugMap
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.SegmentType
import org.openrndr.shape.toQuadratics

fun main() {
    application {

        program {


            val coveragePhrase = """float computeCoverage(float inverseDiameter, vec2 p0, vec2 p1, vec2 p2) {
	if (p0.y > 0.0 && p1.y > 0.0 && p2.y > 0.0) return 0.0;
	if (p0.y < 0.0 && p1.y < 0.0 && p2.y < 0.0) return 0.0;

	// Note: Simplified from abc formula by extracting a factor of (-2) from b.
	vec2 a = p0 - 2.0*p1 + p2;
	vec2 b = p0 - p1;
	vec2 c = p0;

	float t0, t1;
	if (abs(a.y) >= 1e-5) {
		// Quadratic segment, solve abc formula to find roots.
		float radicand = b.y*b.y - a.y*c.y;
		if (radicand <= 0.0) return 0.0;
	
		float s = sqrt(radicand);
		t0 = (b.y - s) / a.y;
		t1 = (b.y + s) / a.y;
	} else {
		// Linear segment, avoid division by a.y, which is near zero.
		// There is only one root, so we have to decide which variable to
		// assign it to based on the direction of the segment, to ensure that
		// the ray always exits the shape at t0 and enters at t1. For a
		// quadratic segment this works 'automatically', see readme.
		float t = p0.y / (p0.y - p2.y);
		if (p0.y < p2.y) {
			t0 = -1.0;
			t1 = t;
		} else {
			t0 = t;
			t1 = -1.0;
		}
	}

	float alpha = 0.0;
	
	if (t0 >= 0.0 && t0 < 1.0) {
		float x = (a.x*t0 - 2.0*b.x)*t0 + c.x;
		alpha += clamp(x * inverseDiameter + 0.5, 0.0, 1.0);
	}

	if (t1 >= 0.0 && t1 < 1.0) {
		float x = (a.x*t1 - 2.0*b.x)*t1 + c.x;
		alpha -= clamp(x * inverseDiameter + 0.5, 0.0, 1.0);
	}

	return alpha;
}
"""
            val slugMap = SlugMap(
                colorBuffer(4096, 1, type = ColorType.FLOAT32, format = ColorFormat.RG),
                colorBuffer(4096, 1, type = ColorType.FLOAT32, format = ColorFormat.RG)
            )

            val vb = vertexBuffer(vertexFormat {
                position(3)
                textureCoordinate(2)
            }, 6)

            val face = loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 500.0, 1.0)

            val glyph = face.glyphForCharacter('8')

            val shape =  glyph.shape()
            val b = shape.bounds

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

            val s = slugMap.coordinates.shadow

            var segments = shape.contours.flatMap {  it.reversed.segments.flatMap { it.toQuadratics(1.0) } }



            for ((index, segment) in segments.withIndex()) {
                val segment = segment.quadratic
                s[index * 3, 0] = ColorRGBa(segment.start.x, segment.start.y, 0.0, 1.0)
                s[index * 3 + 1, 0] = ColorRGBa(segment.control[0].x, segment.control[0].y, 0.0, 1.0)
                s[index * 3 + 2, 0] = ColorRGBa(segment.end.x, segment.end.y, 0.0, 1.0)

            }
            s.upload()

            val si = slugMap.index.shadow

            si[0, 0] = ColorRGBa(0.0, segments.size.toDouble(), 0.0, 1.0)

            si.upload()

            extend {


                drawer.shadeStyle = shadeStyle {

                    fragmentPreamble = """
                        $coveragePhrase
                    """.trimIndent()
                    fragmentTransform = """
                        vec2 index = texelFetch(p_slugIndex, ivec2(0, 0), 0).xy;
                        
                        int segments = int(index.g + 0.5);
                        int base = int(index.r + 0.5);
                        
                        vec2 uv = va_position.xy;

                        vec2 inverseDiameter = 1.0 / (1.0 * fwidth(uv));

                        
                        
                        float alpha = 0.0;
                        for (int s = 0; s < segments; ++s) {
                        
                            int x = base + s * 3;
                            int y = 0;
                        
                            vec2 p0 = texelFetch(p_slugCoords, ivec2(x, y), 0).xy - uv;
                            vec2 p1 = texelFetch(p_slugCoords, ivec2(x + 1, y), 0).xy - uv;
                            vec2 p2 = texelFetch(p_slugCoords, ivec2(x + 2, y), 0).xy - uv;
                            
                            
                            alpha += computeCoverage(1.0 * inverseDiameter.x, p0, p1, p2);
                        }
               
                        
                        alpha = clamp(alpha, 0.0, 1.0);
                        
                        x_fill = vec4(alpha, alpha, 0.05, 1.0);                        
                    """.trimIndent()


                    parameter("slugCoords", slugMap.coordinates)
                    parameter("slugIndex", slugMap.index)
                }

                drawer.translate(0.0, 400.0)
                drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)

//                drawer.defaults()
                drawer.shadeStyle = null
//                drawer.shape(shape)

                drawer.image(slugMap.coordinates)
            }
        }
    }

}