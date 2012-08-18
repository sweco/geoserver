var Process = require("geoscript/process").Process;
var {Feature, Collection, Schema} = require("geoscript/feature");

exports.process = new Process({

    title: "Distance and Bearing",

    description: "Generates features with (cartesian) distance and bearing metrics given an existing feature collection and an origin.",

    inputs: {
        origin: {
            type: "Point",
            title: "Origin",
            description: "The origin from which to calculate distance and bearing."
        },
        features: {
            type: "FeatureCollection",
            title: "Features",
            description: "The features to which distance and bearing should be calculated."
        }
    },

    outputs: {
        result: {
            type: "FeatureCollection",
            title: "Resulting Features",
            description: "Features with calculated distance and bearing attributes."
        }
    },

    run: function(inputs) {
        var origin = inputs.origin;
        var inSchema = inputs.features.schema;

        var schema = new Schema({
            name: "result",
            fields: [
                {name: "geometry", type: inSchema.geometry.type},
                {name: "distance", type: "Double"},
                {name: "bearing", type: "Double"}
            ]
        });

        var collection = new Collection({
            features: function() {
                for (var inFeature in inputs.features) {

                    var point = inFeature.geometry.centroid;
                    var distance = origin.distance(point);
                    var bearing = (270 + Math.atan2(point.y - origin.y, point.x - origin.x) * 180 / Math.PI) % 360;

                    var outFeature = new Feature({
                        schema: schema,
                        properties: {
                            geometry: inFeature.geometry,
                            distance: distance,
                            bearing: bearing
                        }
                    });
                    yield outFeature;
                }
            }
        });

        return {result: collection};
    }

});
