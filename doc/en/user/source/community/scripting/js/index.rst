.. _scripting_js:

JavaScript
==========

.. toctree::
   :hidden:

   api/index

Script Hooks
------------

app
^^^

In JavaScript the app hook is based on 
`JSGI <http://wiki.commonjs.org/wiki/JSGI>`_ which provides a common interface
for JavaScript web application development.  The app script must export a 
function named ``app`` that accepts a ``request`` object and returns a 
``response`` object.

.. code-block:: javascript

    export.app = function(request) {
      // handle the request and return a response
    }

The function must be present in a file named ``main.js`` in a named *application
directory*.  Application directories live under the ``scripts/apps`` directory 
under the root of the data directory::

  GEOSERVER_DATA_DIR/
     ...
     scripts/
       apps/
          app1/
             main.js
             ...
          app2/
             main.js
             ...

The application is web accessible from the path ``/script/apps/{app}`` where 
``{app}`` is the name of the application. All requests that start with this path 
are dispatched to the ``app`` function in ``main.js``.

Hello World Example
~~~~~~~~~~~~~~~~~~~

In this example a simple "Hello World" application is built.  First step is to 
create a directory for the app named ``hello``::

  cd $GEOSERVER_DATA_DIR/scripts/apps
  mkdir hello
  
Next step is to create the ``main.js`` file::

  cd hello
  touch main.js
  
Within the app function the following things will happen:

#. Report an HTTP status code of 200
#. Declare the content type of the response, in this case "text/plain"
#. Generate the body of response, in this case the string "Hello World"

This is accomplished with the following code:

.. code-block:: javascript

    export.app = function(request) {
      return {
        status: 200, // step 1
        headers: {"Content-Type": "text/plain"}, // step 2
        body: ["Hello World"] // step 3
      };
    };


wps
^^^

TODO: complete wps docs

Buffer Example
~~~~~~~~~~~~~~

In this example a simple buffer process is created. First step is to create a file named 
``buffer.py`` in the ``scripts/wps`` directory::

  cd $GEOSERVER_DATA_DIR/scripts/wps
  touch buffer.js
  
TODO: complete buffer example

GeoScript-JS
------------

As mentioned :ref:`previously <scripting_supported_geoscript>` GeoScript provides 
scripting apis for GeoTools in various languages. Naturally the GeoServer 
JavaScript extension comes with GeoScript JavaScript enabled. In the buffer 
example above an example of importing a GeoScript class was shown.

The GeoScript JavaScript api is documented 
`here <http://geoscript.org/js/api/index.html>`__.

API Reference
-------------

In much the same way as GeoScript provides a convenient scripting layer on top of 
GeoTools the JavaScript scripting extension provides a ``geoserver`` JavaScript
module that provides convenient access to some of the GeoServer internals. 

The GeoServer JavaScript api is documented :ref:`here <scripting_javascript_api>`.

