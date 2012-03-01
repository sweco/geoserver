def title(t):
  def wrap(f):
    f.title = t
    return f
  return wrap
  
def description(d):
  def wrap(f):
    f.description = d
    return f
  return wrap
  
def inputs(in):
  def wrap(f):
    f.inputs = in
    return f
  return wrap
  
def outputs(out):
  def wrap(f):
    f.outputs = out
    return f
  return wrap