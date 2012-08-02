from geoscript import core

def process(inputs, outputs, title=None, description=None):
  def wrap(f):
    def wrapped(*args, **kwargs):
      # map arguments on way in
      args = (core.map(a) for a in args)
      for k in kwargs:
        kwargs[k] = core.map(kwargs[k])

      # unmap on way out
      return core.unmap(f(*args, **kwargs))

    wrapped.title = title
    wrapped.description = description

    # unmap the specified inputs and outputs 
    wrapped.inputs = dict((k,_unmap(v)) for k,v in inputs.iteritems())
    wrapped.outputs = dict((k,_unmap(v)) for k,v in outputs.iteritems())
    return wrapped
  return wrap

def _unmap(v):
  return tuple([core.unmap(x) for x in v])
