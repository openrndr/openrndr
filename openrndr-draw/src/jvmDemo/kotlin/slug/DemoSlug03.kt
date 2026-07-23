
package slug

import FontDriverFreetype
import TextShapingDriverHarfBuzz
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.DrawPrimitive
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
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import org.openrndr.shape.SegmentType
import org.openrndr.shape.toQuadratics
import kotlin.math.cos

fun main() {
    application {

        configure {
            width = 720
            height = 720
        }
        program {

            FontDriver.driver = FontDriverFreetype()

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

            val face = loadFace("data/fonts/NotoSansKR-VariableFont_wght.ttf", 80.0, 1.0)

            for (i in face.axes) {
                println("$i ${face.getAxisValue(i)}")
            }
            face.setAxisValue("Weight", 100.0)

            val slugGlyphMap = SlugGlyphMap(slugMap)

            val instances = vertexBuffer(vertexFormat {
                attribute("slugIndex", VertexElementType.FLOAT32)
                attribute("transform", VertexElementType.MATRIX44_FLOAT32)
            }, 10_000)

            val texts = listOf("안녕하세요!", "안녕하세요!", "안녕하세요!", "안녕하세요!", "안녕하세요!", "안녕하세요!")

            val shaper = TextShapingDriverHarfBuzz()
            val shapeResults = texts.map { shaper.shape(face, it) }

            val glyphCount = shapeResults.sumOf { it.size }

            instances.put {

                for ((index, shapeResult) in shapeResults.withIndex()) {
                    var cursor = Vector2(0.0, (index+1) * face.height )
                    face.setAxisValue("Weight", 100.0 + index * 100.0)

                    println(face.hashCode())
                    for ((index, i) in shapeResult.withIndex()) {


                        write(slugGlyphMap.getGlyphForIndex(face, i.glyphIndex).toFloat())
                        write(transform {
                            translate(cursor + shapeResult[index].offset)
                        })
                        cursor += shapeResult[index].advance

                    }

                }
            }

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



            extend {

                drawer.translate(drawer.bounds.center)
                //drawer.scale(cos(seconds) * 2.5 + 3.5)
                drawer.translate(-drawer.bounds.center)
                drawer.shadeStyle = shadeStyle {
                    vertexPreamble = """
                        out vec2 uv;
                        flat out int  v_slugIndex;
                        vec2 getIndexVec2(int slugIndex, int p) {
                            ivec2 ts = textureSize(p_slugIndex, 0);
                            int x = (slugIndex * 3 + p) % ts.x;
                            int y = (ts.y -1) - (slugIndex * 3 + p) / ts.x;
                            return texelFetch(p_slugIndex, ivec2(x, y), 0).xy;
                        }
                    """.trimIndent()
                    vertexTransform = """
                       int slugIndex = int(i_slugIndex + 0.5);
                       v_slugIndex = slugIndex;
                       vec2 bmin = getIndexVec2(slugIndex, 1);
                       vec2 bmax = getIndexVec2(slugIndex, 2);
                       
                       x_position = (i_transform * vec4(mix(bmin, bmax, va_position.xy), 0.0, 1.0) ).xyz;
                       uv = mix(bmin, bmax, va_position.xy);
                       
                    """.trimIndent()

                    fragmentPreamble = """
                        flat in int v_slugIndex;
                        in vec2 uv;
                        
                        vec2 rotate(vec2 v) {
                        	return vec2(v.y, -v.x);
                        }
                        $coveragePhrase
                    """.trimIndent()
                    fragmentTransform = """
                        
                        ivec2 its = textureSize(p_slugIndex, 0);
                        int x = (v_slugIndex*3) % its.x;
                        int y = (its.y - 1) - (v_slugIndex*3) / its.x;
                        vec2 index = texelFetch(p_slugIndex, ivec2(x, y), 0).xy;
                        
                        int segments = int(index.g + 0.5);
                        int base = int(index.r + 0.5);
                        
                        //vec2 uv = v_modelPosition.xy;

                        vec2 inverseDiameter = 1.0 / (1.0 * fwidth(uv));

                        float alpha = 0.0;
                        for (int s = 0; s < segments; ++s) {
                        
                            int x = (base + s) * 3;
                            int y = textureSize(p_slugIndex,0).y - 1;
                        
                        //for (int i = -1; i <= 1; ++i) for (int j = -1; j <= 1; ++j)
                        int i = 0; int j = 0;
                         {
                        
                            vec2 muv = uv + inverseDiameter * vec2(ivec2(i, j)) * 1.0;
                            vec2 p0 = texelFetch(p_slugCoords, ivec2(x, y), 0).xy - uv;
                            vec2 p1 = texelFetch(p_slugCoords, ivec2(x + 1, y), 0).xy - uv;
                            vec2 p2 = texelFetch(p_slugCoords, ivec2(x + 2, y), 0).xy - uv;
                            
                            
                            alpha += (1.0) * computeCoverage(1.0 * inverseDiameter.x, p0, p1, p2);
                            alpha += (1.0) * computeCoverage(1.0 * inverseDiameter.y, rotate(p0), rotate(p1), rotate(p2));
                            
                            }
                        }
               
                        
                        alpha = clamp(alpha * 0.5, 0.0, 1.0);
                        
                        x_fill = vec4(x_fill.rgb, alpha);                        
                    """.trimIndent()


                    parameter("slugCoords", slugMap.coordinates)
                    parameter("slugIndex", slugMap.index)
                }

                drawer.vertexBufferInstances(listOf(vb), listOf(instances), DrawPrimitive.TRIANGLES, glyphCount)

//                drawer.defaults()
                drawer.shadeStyle = null
//                drawer.shape(shape)

            //    drawer.image(slugMap.coordinates)
            }
        }
    }

}