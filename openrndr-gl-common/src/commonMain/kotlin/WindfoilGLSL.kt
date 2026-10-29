package org.openrndr.internal.glcommon

/**
 * The windfoil algorithm: box filtered coverage as a per-pixel winding integral over xy-monotone quadratic pieces,
 * gathered from horizontal bands. Port of https://github.com/texel-org/windfoil-algorithm (Apache-2.0) by Matt
 * DesLauriers, extended with strokes, which use the distance to the pieces within the reach of the stroke.
 */
internal val windfoilFunctions = """
// bands with more than WINDFOIL_SORT_MIN pieces are sorted by descending maximum x, must match WindfoilMap.SORT_MIN
#define WINDFOIL_SORT_MIN 4

// the textures are flat arrays in rows of the width of the texture
vec2 windfoilCurve(int texel) {
    int width = textureSize(u_windfoilCurves, 0).x;
    return texelFetch(u_windfoilCurves, ivec2(texel % width, texel / width), 0).xy;
}

uvec4 windfoilRow(int texel) {
    int width = textureSize(u_windfoilRows, 0).x;
    return texelFetch(u_windfoilRows, ivec2(texel % width, texel / width), 0);
}

// the first piece and the number of pieces of a band
ivec2 windfoilBandPieces(int band) {
    uvec4 row = windfoilRow(band * 2);
    return ivec2(int(row.x), int(row.y));
}

// the winding density and the x range of the pieces of a band
vec3 windfoilBandProfile(int band) {
    uvec4 row0 = windfoilRow(band * 2);
    uvec4 row1 = windfoilRow(band * 2 + 1);
    return vec3(uintBitsToFloat(row0.z), uintBitsToFloat(row0.w), uintBitsToFloat(row1.x));
}

// period-2 triangle wave, folds a signed winding number to even-odd coverage
float windfoilTriangleWave(float t) {
    return 1.0 - abs(1.0 - (t - 2.0 * floor(t * 0.5)));
}

// folds the pixel averaged winding number to coverage: nonzero (0) saturates, even-odd (1) folds
float windfoilFold(float f, int fillRule) {
    return fillRule == 1 ? windfoilTriangleWave(f) : min(abs(f), 1.0);
}

// solves a2 t² + a1 t + a0 = v for t in [0, 1] on a monotone quadratic with q(1) = e1, saturating at the endpoints
float windfoilMonotoneRoot(float a2, float a1, float a0, float e1, float v, bool rising) {
    if (rising) {
        if (a0 >= v) return 0.0;
        if (e1 <= v) return 1.0;
    } else {
        if (a0 <= v) return 0.0;
        if (e1 >= v) return 1.0;
    }
    float c = a0 - v;
    if (abs(a2) < 1e-12 * max(abs(a1), 1.0)) {
        return clamp(-c / a1, 0.0, 1.0);
    }
    float discriminant = max(a1 * a1 - 4.0 * a2 * c, 0.0);
    float sq = sqrt(discriminant);
    float qq = -0.5 * (a1 + (a1 >= 0.0 ? sq : -sq));
    bool useR1 = (a1 < 0.0) == rising;
    float num = useR1 ? qq : c;
    float den = useR1 ? a2 : qq;
    return den != 0.0 ? clamp(num / den, 0.0, 1.0) : 0.0;
}

// the exact integral of (x(t) + hx) y'(t) over [ta, tb], a cubic for which the midpoint rule is exact
float windfoilIntegrateInside(vec2 a2, vec2 a1, float x0, float ta, float tb, float hx) {
    if (tb <= ta) return 0.0;
    float tm = 0.5 * (ta + tb);
    float d = 0.5 * (tb - ta);
    float xMid = (a2.x * tm + a1.x) * tm + x0 + hx;
    vec2 dMid = 2.0 * a2 * tm + a1;
    return 2.0 * d * xMid * dMid.y + (2.0 * d * d * d / 3.0) * (a2.x * dMid.y + 2.0 * a2.y * dMid.x);
}

// integrates clamp(x(t), -hx, hx) + hx over the part of a piece within [lo, hi] in y, through the zones left of
// the pixel (no area), inside it (exact) and right of it (full width), with the four clip roots solved at once
float windfoilIntegratePiece(vec2 q1, vec2 q2, vec2 q3, float lo, float hi, float hx) {
    vec2 a2 = q1 - 2.0 * q2 + q3;
    vec2 a1 = 2.0 * (q2 - q1);
    bool yRising = q3.y >= q1.y;
    bool xRising = q3.x >= q1.x;
    vec4 A2 = a2.yyxx;
    vec4 A1 = a1.yyxx;
    vec4 V = vec4(yRising ? vec2(lo, hi) : vec2(hi, lo), -hx, hx);
    bvec4 R = bvec4(yRising, yRising, xRising, xRising);
    vec4 SG = mix(vec4(-1.0), vec4(1.0), R);

    vec4 C = q1.yyxx - V;
    bvec4 saturated0 = greaterThanEqual(C * SG, vec4(0.0));
    bvec4 saturated1 = lessThanEqual((q3.yyxx - V) * SG, vec4(0.0));
    vec4 sq = sqrt(max(A1 * A1 - 4.0 * A2 * C, vec4(0.0)));
    vec4 qq = -0.5 * (A1 + mix(-sq, sq, greaterThanEqual(A1, vec4(0.0))));
    bvec4 useR1 = equal(lessThan(A1, vec4(0.0)), R);
    vec4 num = mix(C, qq, useR1);
    vec4 den = mix(qq, A2, useR1);
    bvec4 valid = notEqual(den, vec4(0.0));
    vec4 tRaw = clamp(mix(vec4(0.0), num / mix(vec4(1.0), den, valid), valid), vec4(0.0), vec4(1.0));
    vec4 T = mix(mix(tRaw, vec4(1.0), saturated1), vec4(0.0), saturated0);

    if (T.y <= T.x) return 0.0;
    vec2 tc = clamp(T.zw, T.xx, T.yy);
    // zones in sweep order: left, inside, right when x rises, mirrored when it falls
    float t1 = xRising ? tc.x : tc.y;
    float t2 = max(xRising ? tc.y : tc.x, t1);
    float area = windfoilIntegrateInside(a2, a1, q1.x, t1, t2, hx);
    vec2 right = xRising ? vec2(t2, T.y) : vec2(T.x, t1);
    float d = max(right.y - right.x, 0.0);
    float tm = 0.5 * (right.x + right.y);
    area += d * (2.0 * a2.y * tm + a1.y) * (2.0 * hx);
    return area;
}

// the signed y span of a piece clipped to [lo, hi], adjacent pieces telescope exactly
float windfoilClippedDy(float y1, float y3, float lo, float hi) {
    return clamp(y3, lo, hi) - clamp(y1, lo, hi);
}

// the winding integral of the pieces of a band over the pixel box [-hx, hx] x [lo, hi] around rc
float windfoilIntegrateBand(ivec2 pieces, vec2 rc, float lo, float hi, float sx) {
    float area = 0.0;
    float hx = sx * 0.5;
    bool sorted = pieces.y > WINDFOIL_SORT_MIN;
    float coordinateUlp = max(abs(rc.x), abs(rc.y)) * 1.2e-7;
    for (int i = 0; i < pieces.y; ++i) {
        int base = (pieces.x + i) * 3;
        vec2 q1 = windfoilCurve(base) - rc;
        vec2 q3 = windfoilCurve(base + 2) - rc;
        // the control point of a monotone piece lies in the hull of its endpoints, it is read after culling
        float xHullMax = max(q1.x, q3.x);
        if (xHullMax <= -hx) {
            if (sorted) break;
            continue;
        }
        float yLo = max(lo, min(q1.y, q3.y));
        float yHi = min(hi, max(q1.y, q3.y));
        if (yHi <= yLo) continue;
        float xHullMin = min(q1.x, q3.x);
        if (xHullMin >= hx) {
            area += sx * windfoilClippedDy(q1.y, q3.y, lo, hi);
            continue;
        }
        if (xHullMax - xHullMin + abs(q3.y - q1.y) <= coordinateUlp * 16.0) {
            float xm = clamp((q1.x + q3.x) * 0.5, -hx, hx) + hx;
            area += xm * windfoilClippedDy(q1.y, q3.y, lo, hi);
            continue;
        }
        vec2 q2 = windfoilCurve(base + 1) - rc;
        area += windfoilIntegratePiece(q1, q2, q3, yLo, yHi, hx);
    }
    return area;
}

// the band of a y offset from the band origin, the same mapping as WindfoilMap uses for filing
int windfoilBandIndex(float dy, float inverseBandHeight, int bandCount) {
    return int(clamp(floor(dy * inverseBandHeight), 0.0, float(bandCount) - 1.0));
}

// the winding integral of a shape over the pixel box rc ± s / 2, gathered from the bands the box overlaps, with
// windows relative to rc
float windfoilIntegrateFace(int rowBase, int bandCount, vec2 bandScale, float y0, vec2 rc, vec2 s) {
    float sy2 = s.y * 0.5;
    float dy0 = y0 - rc.y;
    int first = windfoilBandIndex(-dy0 - sy2, bandScale.y, bandCount);
    int last = windfoilBandIndex(-dy0 + sy2, bandScale.y, bandCount);
    float f = 0.0;
    for (int band = first; band <= last; ++band) {
        float lo = -sy2;
        float hi = sy2;
        if (bandCount > 1) {
            vec2 edges = dy0 + vec2(float(band), float(band) + 1.0) * bandScale.x;
            lo = max(lo, edges.x);
            hi = min(hi, edges.y);
        }
        if (hi <= lo) continue;
        f += windfoilIntegrateBand(windfoilBandPieces(rowBase + band), rc, lo, hi, s.x);
    }
    return f;
}

// the winding integral of a shape over the pixel box from the winding densities of its bands, for shapes that are
// only a few pixels in size
float windfoilProfileFace(int rowBase, int bandCount, vec2 bandScale, vec4 bounds, vec2 rc, vec2 s) {
    vec2 lo = rc - s * 0.5;
    vec2 hi = rc + s * 0.5;
    if (min(hi.x, bounds.z) - max(lo.x, bounds.x) <= 0.0) return 0.0;
    int first = windfoilBandIndex(lo.y - bounds.y, bandScale.y, bandCount);
    int last = windfoilBandIndex(hi.y - bounds.y, bandScale.y, bandCount);
    float ink = 0.0;
    for (int band = first; band <= last; ++band) {
        vec3 profile = windfoilBandProfile(rowBase + band);
        vec2 edges = bounds.y + vec2(float(band), float(band) + 1.0) * bandScale.x;
        float oy = max(min(hi.y, edges.y) - max(lo.y, edges.x), 0.0);
        float ox = max(min(hi.x, profile.z) - max(lo.x, profile.y), 0.0);
        ink += profile.x * oy * ox;
    }
    return ink;
}

// the signed winding number (x) and the number of crossings (y) of a ray in +x from rc + p
ivec2 windfoilWindingAt(int rowBase, int bandCount, vec2 bandScale, float y0, vec2 rc, vec2 p) {
    int band = windfoilBandIndex(p.y + rc.y - y0, bandScale.y, bandCount);
    ivec2 pieces = windfoilBandPieces(rowBase + band);
    int winding = 0;
    int crossings = 0;
    for (int i = 0; i < pieces.y; ++i) {
        int base = (pieces.x + i) * 3;
        vec2 q1 = windfoilCurve(base) - rc;
        vec2 q3 = windfoilCurve(base + 2) - rc;
        bool rising = q3.y > q1.y;
        // half-open, joins count once and extrema not at all
        if (p.y < min(q1.y, q3.y) || p.y >= max(q1.y, q3.y)) continue;
        vec2 q2 = windfoilCurve(base + 1) - rc;
        vec2 a2 = q1 - 2.0 * q2 + q3;
        vec2 a1 = 2.0 * (q2 - q1);
        float t = windfoilMonotoneRoot(a2.y, a1.y, q1.y, q3.y, p.y, rising);
        float x = (a2.x * t + a1.x) * t + q1.x;
        if (x > p.x) {
            crossings += 1;
            winding += rising ? 1 : -1;
        }
    }
    return ivec2(winding, crossings);
}

// the fraction of a grid of samples x samples points over the pixel box that is inside the shape
float windfoilExactCoverage(int rowBase, int bandCount, vec2 bandScale, float y0, int fillRule, vec2 rc, vec2 s, int samples) {
    float inverse = 1.0 / float(samples);
    int inside = 0;
    for (int j = 0; j < samples; ++j) {
        for (int i = 0; i < samples; ++i) {
            vec2 offset = (vec2(float(i), float(j)) + 0.5) * inverse - 0.5;
            ivec2 wk = windfoilWindingAt(rowBase, bandCount, bandScale, y0, rc, offset * s);
            bool hit = fillRule == 1 ? (wk.y & 1) == 1 : wk.x != 0;
            inside += hit ? 1 : 0;
        }
    }
    return float(inside) / float(samples * samples);
}

// the distance from rc to the nearest piece within reach, from the bands within reach. Pieces beyond the nearest
// piece found so far are skipped, sorted bands stop at the first piece that is further left.
float windfoilDistance(int rowBase, int bandCount, vec2 bandScale, float y0, vec2 rc, float reach) {
    float minDist = 1e10;
    int first = windfoilBandIndex(rc.y - reach - y0, bandScale.y, bandCount);
    int last = windfoilBandIndex(rc.y + reach - y0, bandScale.y, bandCount);
    for (int band = first; band <= last; ++band) {
        ivec2 pieces = windfoilBandPieces(rowBase + band);
        bool sorted = pieces.y > WINDFOIL_SORT_MIN;
        for (int i = 0; i < pieces.y; ++i) {
            float r = min(reach, minDist);
            int base = (pieces.x + i) * 3;
            vec2 q1 = windfoilCurve(base) - rc;
            vec2 q3 = windfoilCurve(base + 2) - rc;
            if (max(q1.x, q3.x) < -r) {
                if (sorted) break;
                continue;
            }
            if (min(q1.x, q3.x) > r || min(q1.y, q3.y) > r || max(q1.y, q3.y) < -r) continue;
            vec2 q2 = windfoilCurve(base + 1) - rc;
            minDist = min(minDist, quad_bezier_distance(vec2(0.0), q1, q2, q3));
        }
    }
    return minDist;
}
""".trimIndent()

/**
 * Dilation of the shape quad such that every pixel whose box overlaps the shape (or its stroke) is rasterized:
 * half a pixel for the box filter plus slack for the derivatives, and the stroke outset in object space.
 */
internal val windfoilDilate = """
vec2 WindfoilDilate(vec4 pos, vec2 tex, vec4 jac, vec4 m0, vec4 m1, vec4 m3, vec2 dim, out vec2 vpos) {
    float strokeOutset = 0.0;
    if (i_strokeWeight > 0.0 && i_stroke.a > 0.0) {
        if (i_strokeMode == 0) { // CENTER
            strokeOutset = i_strokeWeight * 0.5;
        } else if (i_strokeMode == 2) { // OUTSIDE
            strokeOutset = i_strokeWeight;
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

    // half a pixel of dilation in viewport space, 0.625 pixels with slack
    vec2 d = pos.zw * (s2 * (st + sqrt(uv)) / (uv - st * st));
    d = d * 1.25 + pos.zw * strokeOutset;

    vpos = pos.xy + d;
    return (vec2(tex.x + dot(d, jac.xy), tex.y + dot(d, jac.zw)));
}
""".trimIndent()
