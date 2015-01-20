# area-mapper

## Callout intent input fields:
* accuracy: String of integer meters; locations less accurate than this are ignored
* coordinates: "true" if coordinates required in result
* image: "true" if image file path required in result
* interval_meters: String of integer meters; new location less than this far away from the previous location will be ignored
* interval_millis: String of integer milliseconds; new location less than this much newer than previous location will be ignored

## Result intent fields (all under bundle "odk_intent_bundle"):
* area: String of double square meters; area of measured land
* coordinates: (may be absent) String of lat/lng doubles; perimeter coordinates of measured land
* image: (may be absent) String of file path: path to saved image of land map
