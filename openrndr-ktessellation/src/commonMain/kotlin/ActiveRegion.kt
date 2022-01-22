package org.openrndr.ktessellation

internal class ActiveRegion {
    var eUp /* upper edge, directed right to left */: GLUhalfEdge? = null
    var nodeUp /* dictionary node corresponding to eUp */: DictNode? = null
    var windingNumber /* used to determine which regions are
                                 * inside the polygon */ = 0
    var inside /* is this region inside the polygon? */ = false
    var sentinel /* marks fake edges at t = +/-infinity */ = false
    var dirty /* marks regions where the upper or lower
                                 * edge has changed, but we haven't checked
                                 * whether they intersect yet */ = false
    var fixUpperEdge /* marks temporary edges introduced when
                                 * we process a "right vertex" (one without
                                 * any edges leaving to the right) */ = false
}
