from geoscript.geom import Geometry

def run(geom, distance):
  return geom.buffer(distance);

run.title = 'Buffer'
run.description = 'Buffers a geometry'
run.inputs = [(Geometry, 'The geometry to buffer'), (float,'The buffer distance')]
run.outputs = [('result', Geometry, 'The buffered geometry')]