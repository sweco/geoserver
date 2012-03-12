import base64, code, httplib, logging, optparse
try:
  import json
except ImportError:
  try:
    import simplejson as json
  except ImportError, e:
    print e 
    exit('Install simplejson or run on Python 2.6+')

logging.basicConfig(format='%(levelname)s - %(message)s')
logger = logging.getLogger('session')

class SessionClient(object):

  def __init__(self, host, port=8080, context='geoserver', 
               user='admin', passwd='geoserver'):

    self.cx = httplib.HTTPConnection(host, port)
    self.auth = base64.encodestring('Basic %s:%s' % (user,passwd)).replace('\n','')
    self.context = context
  
  def list(self):
    r = self._request('GET', 'sessions/py') 
    if self._check(r, 200, 'GET sessions failed'):
      obj = json.loads(r.read())
      return [(s['id'],s['engine']) for s in obj['sessions']]

  def new(self):
    r = self.request('POST', 'sessions/py')
    if self._check(r, 201, 'POST new session failed'):
      return Session(int(r.read()), self)

  def bind(self, sid):
    r = self.request('GET', 'sessions/py/%d' % sid)
    if self._check(r, 200, 'GET session failed'):
      return Session(sid, self)
    
  def close(self):
     self.cx.close()

  def _request(self, method, path, body=None):
    self.cx.request(method, '/geoserver/script/%s' % path, body, 
      {'Authorization':self.auth})
    return self.cx.getresponse()

  def _check(self, resp, status, msg):
    if resp.status != status:
      logger.warning('%s, expecting status %d but got %d' 
        % (msg, status, resp.status))
      return False
    return True

class Session(object):
  
  def __init__(self, sid, client):
    self.sid = sid
    self.client = client

  def eval(self, input): 
    r = self.client._request('PUT', 'sessions/py/%d' % self.sid, input)
    return r.read() 

if __name__ == '__main__':
  p = optparse.OptionParser('Usage: %prog [options] host session')
  p.add_option('-p', '--port', dest='port', type='int', default=8080, 
               help='server port, default is 8080')
  p.add_option('-u', '--user', dest='user', default='admin',
               help='username, default is admin')
  p.add_option('-w', '--password', dest='passwd', default='geoserver',
               help='password, default is geoserver')
  p.add_option('-c', '--context', dest='context', default='geoserver', 
               help='context, default is geoserver')
 
  opts, args = p.parse_args()
  if len(args) == 0:
     p.error('host is required')

  c = SessionClient(args[0], **vars(opts))
  if len(args) == 1:
    print c.list()

  """
  while True: 
    buf = raw_input('>>> ')
    try:
      while not code.compile_command(buf):
        buf = buf + '\n' + raw_input('... ')

      if "exit()" == buf :
        c.close()
        break

      result = c.eval(buf)
      if result and len(result.strip()) > 0:
        print result,
        if not result[-1] == '\n':
          print
    except SyntaxError, e:
      print e  

  """
