package org.openrndr.draw.slug

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Drawer
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord

@JvmRecord
data class SlugCommand(val slugIndex: Int, val transform: Matrix44, val fill: ColorRGBa?, val stroke: ColorRGBa?, val strokeWeight: Double, val strokeMode : Int = 0)


class SlugDrawer {
    val vb = vertexBuffer(vertexFormat {
        position(3)
        textureCoordinate(2)
    }, 6)

    val b = Rectangle(0.0, 0.0, 1.0, 1.0)

    init {
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
    }

    val instances = vertexBuffer(vertexFormat {
        attribute("transform", VertexElementType.MATRIX44_FLOAT32)
        attribute("slugBounds", VertexElementType.VECTOR4_FLOAT32)
        attribute("fill", VertexElementType.VECTOR4_FLOAT32)
        attribute("stroke", VertexElementType.VECTOR4_FLOAT32)
        attribute("strokeWeight", VertexElementType.FLOAT32)
        attribute("strokeMode", VertexElementType.INT32)
        attribute("bandIndex", VertexElementType.VECTOR2_INT32)
        attribute("bandCount", VertexElementType.INT32)
    }, 10_000)


    var slugCount = 0

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


    fun prepare(slugMap: SlugMap, slugCommands: List<SlugCommand>) {
        instances.put {
            for (command in slugCommands) {
                write(command.transform)

                run {
                    val bounds = slugMap.bounds[command.slugIndex]
                    val minmax = Vector4(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height)
                    write(minmax)
                }

                write(command.fill ?: ColorRGBa.TRANSPARENT)
                write(command.stroke ?: ColorRGBa.TRANSPARENT)
                write(command.strokeWeight.toFloat())
                write(command.strokeMode)
                write(slugMap.bandIndices[command.slugIndex])
                write(slugMap.bandCounts[command.slugIndex])

            }
        }
        slugCount = slugCommands.size
    }

    fun draw(drawer: Drawer, slugMap: SlugMap) {


        val bezDist = """// ---------------------------------------------------------------------------
// Fast distance to a quadratic Bezier curve on the GPU
//
// Derived using the same method as:
// https://blog.pkh.me/p/46-fast-calculation-of-the-distance-to-cubic-bezier-curves-on-the-gpu.html
//
// For a cubic Bezier, D'(t)=0 is a degree-5 polynomial (needs the full
// Cem Yuksel cascade + bisection solver described in the article).
// For a quadratic Bezier, D'(t)=0 is only a *cubic*, so we need just one
// level of the same cascade idea:
//
//   1. Differentiate the cubic once more -> quadratic, solved analytically.
//   2. Use its (<=2) roots to split [0,1] into monotonic sub-intervals.
//   3. Newton-bisect the cubic on each sub-interval where it changes sign.
//
// At most 3 roots + the 2 endpoints = 5 candidate distances to test.
// ---------------------------------------------------------------------------

const float BEZ_EPS = 1e-6;

// Return true if x is not NaN nor infinite. highp is required for IEEE 754
// bit-pattern semantics to be reliable.
bool bz_isfinite(highp float x) {
    return (floatBitsToUint(x) & 0x7f800000u) != 0x7f800000u;
}

// ---------------------------------------------------------------------------
// Quadratic root solver: solve A*t^2 + B*t + C = 0, keeping only roots in
// [0,1], sorted ascending. Returns the number of roots found (0, 1 or 2).
//
// Uses the Numerical-Recipes-style formula (avoids the classic (-b+-sqrt)/2a
// cancellation issue) with an explicit fallback to the linear case when A
// vanishes (this happens for a quadratic Bezier whose control point sits
// exactly on the segment P0-P2, i.e. a degenerate straight line).
// ---------------------------------------------------------------------------
int bz_root_find2(out float r[3], float A, float B, float C) {
    int count = 0;

    if (abs(A) < 1e-12) {
        // Linear fallback: B*t + C = 0
        float s = -C / B;
        if (bz_isfinite(s) && s >= 0.0 && s <= 1.0) r[count++] = s;
        return count;
    }

    float D = B*B - 4.0*A*C;
    if (D < 0.0) return count; // no real root

    float h = sqrt(D);
    float q = -0.5 * (B + (B > 0.0 ? h : -h));
    vec2 v = vec2(q / A, C / q);
    if (v.x > v.y) v = v.yx; // keep ordered ascending

    if (bz_isfinite(v.x) && v.x >= 0.0 && v.x <= 1.0) r[count++] = v.x;
    if (bz_isfinite(v.y) && v.y >= 0.0 && v.y <= 1.0) r[count++] = v.y;
    return count;
}

// Evaluate the cubic A*t^3 + B*t^2 + C*t + D via Horner's method.
float bz_poly3(float A, float B, float C, float D, float t) {
    return ((A*t + B)*t + C)*t + D;
}

// ---------------------------------------------------------------------------
// Newton-bisection for a single root of the cubic inside [t.x, t.y], given
// v = (f(t.x), f(t.y)) with opposite signs (or a zero). Same construction as
// the article's bisect5(), just unrolled for degree 3: a single pass of
// Horner's method computes both f(x) (y) and f'(x) (q) simultaneously.
// ---------------------------------------------------------------------------
float bz_bisect3(float A, float B, float C, float D, vec2 t, vec2 v) {
    float x = (t.x + t.y) * 0.5;
    float s = v.x < v.y ? 1.0 : -1.0;

    for (int i = 0; i < 24; i++) {
        float y = A*x + B, q = A*x + y;
              y = y*x + C; q = q*x + y;
              y = y*x + D;

        t = (s*y < 0.0) ? vec2(x, t.y) : vec2(t.x, x);

        float next = x - y / q; // Newton step
        next = (next >= t.x && next <= t.y) ? next : (t.x + t.y) * 0.5;

        if (abs(next - x) < BEZ_EPS) return next;
        x = next;
    }
    return x;
}

// ---------------------------------------------------------------------------
// Cubic root solver restricted to [0,1]: solve A*t^3+B*t^2+C*t+D = 0.
// Falls back to the quadratic solver if A is negligible (degenerate curve).
// Returns up to 3 roots.
// ---------------------------------------------------------------------------
int bz_root_find3(out float r[3], float A, float B, float C, float D) {
    if (abs(A) < 1e-9) {
        return bz_root_find2(r, B, C, D);
    }

    float r2[3];
    // Derivative of the cubic: 3A t^2 + 2B t + C
    int n = bz_root_find2(r2, 3.0*A, 2.0*B, C);

    int count = 0;
    vec2 p = vec2(0.0, bz_poly3(A, B, C, D, 0.0));

    for (int i = 0; i <= n; i++) {
        float x = (i == n) ? 1.0 : r2[i];
        float y = bz_poly3(A, B, C, D, x);

        if (p.y * y <= 0.0) {
            r[count++] = bz_bisect3(A, B, C, D, vec2(p.x, x), vec2(p.y, y));
        }
        p = vec2(x, y);
    }
    return count;
}

// ---------------------------------------------------------------------------
// Distance from point p to the quadratic Bezier curve (p0, p1, p2).
// ---------------------------------------------------------------------------
float quad_bezier_distance(vec2 p, vec2 p0, vec2 p1, vec2 p2) {
    // Endpoints (t=0 and t=1) are always valid candidates.
    vec2 dp0 = p0 - p, dp2 = p2 - p;
    float dist2 = min(dot(dp0, dp0), dot(dp2, dp2));

    // Quadratic Bezier -> monomial coefficients
    vec2 a = p0 - 2.0*p1 + p2;
    vec2 b = 2.0 * (p1 - p0);
    vec2 dvec = p0 - p; // c - p, since c = p0

    // Coefficients of D'(t)/2 = A t^3 + B t^2 + C t + D
    float A = 2.0 * dot(a, a);
    float B = 3.0 * dot(a, b);
    float C = dot(b, b) + 2.0 * dot(a, dvec);
    float Dc = dot(b, dvec);

    float roots[3];
    int count = bz_root_find3(roots, A, B, C, Dc);

    for (int i = 0; i < count; i++) {
        float t = roots[i];
        vec2 dp = (a * t + b) * t + dvec; // B(t) - p
        dist2 = min(dist2, dot(dp, dp));
    }

    return sqrt(dist2);
}
"""


        drawer.isolated {
            drawer.shadeStyle = shadeStyle {

                vertexPreamble = """
vec2 SlugDilate(vec4 pos, vec2 tex, vec4 jac, vec4 m0, vec4 m1, vec4 m3, vec2 dim, out vec2 vpos) {

    float expansion = 1.0;
    
    if (i_strokeWeight > 0.0 && i_stroke.a > 0.0) {
        if (i_strokeMode == 2) { // OUTER
            expansion += i_strokeWeight * 4.0;
        } else if (i_strokeMode == 0) { // CENTER 
            expansion += i_strokeWeight * 2.0;
        } 
    }
    

	vec2 n = normalize(pos.zw);
	float s = dot(m3.xy, pos.xy) + m3.w;
	float t = dot(m3.xy, n);

	float u = (s * dot(m0.xy, n) - t * (dot(m0.xy, pos.xy) + m0.w)) * dim.x;
	float v = (s * dot(m1.xy, n) - t * (dot(m1.xy, pos.xy) + m1.w)) * dim.y;

	float s2 = s * s;
	float st = s * t;
	float uv = u * u + v * v;   
	vec2 d = pos.zw * (s2 * (st + sqrt(uv)) / (uv - st * st));
    d *= expansion;

	vpos = pos.xy + d;
	return (vec2(tex.x + dot(d, jac.xy), tex.y + dot(d, jac.zw)));
}                    

                    out vec2 v_dilatedTexCoord;
                """.trimIndent()
                vertexTransform = """
                    vec2 p;
                    x_position = vec3(mix(i_slugBounds.xy, i_slugBounds.zw, a_texCoord0.xy), 0.0);
                    mat4 mvp = u_projectionMatrix * u_viewMatrix * i_transform;
                    
                    vec2 nuv = a_texCoord0.xy;
                    
                    vec2 norm = (nuv - vec2(0.5, 0.5)) * 2.0;
                    
                    //norm = normalized(norm);
                    vec2 dims = i_slugBounds.zw - i_slugBounds.xy;
                    vec4 pos = vec4(x_position.xy, norm);
                    
                    mat4 emo = mat4(vec4(dims.x, 0.0, 0.0, 0.0), vec4(0.0, dims.y, 0.0, 0.0), vec4(0.0, 0.0, 1.0, 0.0), vec4(i_slugBounds.xy, 0.0, 1.0));
                    
                    
                    mat4 it = inverse(i_transform * emo);
                    vec4 jac = vec4(it[0][0], it[1][0], it[0][1], it[1][1]);
                    
                    mat4 mvpt = transpose(mvp);
                    vec4 m0 = mvpt[0];
                    vec4 m1 = mvpt[1];
                    vec4 m2 = mvpt[2];
                    vec4 m3 = mvpt[3];
                    
                    //float2 SlugDilate(float4 pos, float2 tex, float4 jac, float4 m0, float4 m1, float4 m3, float2 dim, out float2 vpos)
                    // pos.xy = object-space vertex coordinates.
                    // pos.zw = object-space normal vector.

                    // tex.xy = em-space sample coordinates.
                    // tex.z = location of glyph data in band texture (interpreted as integer):
                    // jac - upper left part of i_transform
                        
                        vec2 dim = vec2(1440.0, 1440.0);

                    
                    v_dilatedTexCoord = SlugDilate(pos, a_texCoord0, jac, m0, m1, m3, dim, p);
                        
                        x_position = (i_transform * vec4(p, 0.0, 1.0) ).xyz;
                       // x_position = (i_transform * vec4(x_position, 1.0)).xyz;
                    """

                fragmentPreamble = """
                    in vec2 v_dilatedTexCoord;
                    ${bezDist}
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
                        vec2 slugPosition = mix(vi_slugBounds.xy, vi_slugBounds.zw, v_dilatedTexCoord.xy);
       
                        vec2 emsPerPixel = fwidth(slugPosition);
	                    vec2 pixelsPerEm = 1.0 / emsPerPixel;
                        
                        
                        ivec2 bandIndex = ivec2(floor(v_dilatedTexCoord.xy * float(vi_bandCount) ));
                        bandIndex = clamp(bandIndex, ivec2(0), ivec2(vi_bandCount - 1));
                        
                        int hCurveCount = 0;
                        int hCurveIndex = 0;
                        int hBand = 0;                        
                        readBandHeader(vi_bandIndex, bandIndex.y, hCurveCount, hCurveIndex, hBand);
                        
                        float xcov = 0.0;
                        float xwgt = 0.0;

                        float xcovs[FILTER_SAMPLES] = float[](0.0, 0.0, 0.0, 0.0, 0.0);
                        float xwgts[FILTER_SAMPLES] = float[](0.0, 0.0, 0.0, 0.0, 0.0);


                        float minDist = 1E10;

                        for (int i = 0; i < hCurveCount; i++) {
                            ivec2 ascending = ivec2(0, 0);
                            ivec2 descending = ivec2(0, 0);
                            readBandCurveIndex(hCurveIndex + i, ascending, descending);
                            
                            vec2 a, b, c;
                            readCurve(descending, a, b, c);
                            
                            if (vi_stroke.a > 0.0 && vi_strokeWeight > 0.0) {
                                float dist = quad_bezier_distance(slugPosition, a, b, c);
                                
                                if (dist < minDist) {
                                    minDist = dist;
                                }
                            }
                            
                            if (vi_fill.a > 0.0 || (vi_strokeMode&3) != 0) 
                            for (int s = 0; s < FILTER_SAMPLES; ++s) {
                                vec2 o = float(s-FILTER_SAMPLES/2) * vec2(0.0, emsPerPixel.y * FILTER_SPREAD);
                                vec4 p12 = vec4(a, b) - vec4(slugPosition + o, slugPosition + o);
                                vec2 p3 = c - (slugPosition + o);
                                
                                if (max(max(p12.x, p12.z), p3.x) * pixelsPerEm.x < -0.5) break;
                                
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
                             
                            if (vi_stroke.a > 0.0 && vi_strokeWeight > 0.0) {
                                float dist = quad_bezier_distance(slugPosition, a, b, c);
                                
                                if (dist < minDist) {
                                    minDist = dist;
                                }
                            }
                            
                            if (vi_fill.a > 0.0 || (vi_strokeMode&3) != 0)
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
                        
                        float strokeCoverage = 0.0;
                        
                        if (vi_stroke.a > 0.0 && vi_strokeWeight > 0.0) {
                            float w = fwidth(minDist);
                            if (vi_strokeMode == 0) {
                                strokeCoverage = smoothstep(vi_strokeWeight/2.0 + w, vi_strokeWeight/2.0 -w, minDist);
                            } else if (vi_strokeMode == 1){
                                strokeCoverage = smoothstep(vi_strokeWeight + w, vi_strokeWeight-w, minDist) * coverage;
                            } else if (vi_strokeMode == 2){
                                strokeCoverage = smoothstep(vi_strokeWeight + w, vi_strokeWeight-w, minDist) * (1.0 - coverage);
                            } else if (vi_strokeMode == 3) { // ERODE
                                strokeCoverage = strokeCoverage = smoothstep(vi_strokeWeight + w, vi_strokeWeight-w, minDist);
                                coverage *= (1.0 - strokeCoverage);
                                strokeCoverage = 0.0;
                            }
                        }
                        x_fill = vi_fill;;
                        x_fill.a *= coverage;
                        x_fill *= (1.0 - strokeCoverage);
                        x_fill += vi_stroke * strokeCoverage;
                    """
                parameter("bands", slugMap.bands)
                parameter("curves", slugMap.curves)
            }
            drawer.vertexBufferInstances(listOf(vb), listOf(instances), DrawPrimitive.TRIANGLES, slugCount)
        }
    }

}