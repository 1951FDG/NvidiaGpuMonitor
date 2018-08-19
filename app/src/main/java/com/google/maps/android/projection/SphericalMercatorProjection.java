/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.projection;

import android.support.annotation.NonNull;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.geometry.Point;

public class SphericalMercatorProjection {
    private final double mWorldWidth;

    public SphericalMercatorProjection(final double worldWidth) {
        mWorldWidth = worldWidth;
    }

    @NonNull
    public LatLng toLatLng(@NonNull Point point) {
        final double x = point.x / mWorldWidth - 0.5;
        final double lng = x * 360;

        double y = .5 - (point.y / mWorldWidth);
        final double lat = 90 - Math.toDegrees(Math.atan(Math.exp(-y * 2 * Math.PI)) * 2);

        return new LatLng(lat, lng);
    }
}
