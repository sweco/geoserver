class GeoServer(object):
  """
  The GeoServer configuration. 
  """
  def __init__(self):
     try:
       from org.geoserver.platform import GeoServerExtensions
     except ImportError:
       pass
     else:
       self._geoserver = GeoServerExtensions.bean('geoServer') 

  def getcatalog(self):
     from geoserver.catalog import Catalog
     return Catalog()
  catalog = property(getcatalog)
