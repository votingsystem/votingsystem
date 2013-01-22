package javax.jnlp;

import java.net.URL;

public abstract interface BasicService
{
  public abstract URL getCodeBase();

  public abstract boolean isOffline();

  public abstract boolean showDocument(URL paramURL);

  public abstract boolean isWebBrowserSupported();
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.BasicService
 * JD-Core Version:    0.6.0
 */