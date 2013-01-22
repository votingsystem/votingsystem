package javax.jnlp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract interface PersistenceService
{
  public static final int CACHED = 0;
  public static final int TEMPORARY = 1;
  public static final int DIRTY = 2;

  public abstract long create(URL paramURL, long paramLong)
    throws MalformedURLException, IOException;

  public abstract FileContents get(URL paramURL)
    throws MalformedURLException, IOException, FileNotFoundException;

  public abstract void delete(URL paramURL)
    throws MalformedURLException, IOException;

  public abstract String[] getNames(URL paramURL)
    throws MalformedURLException, IOException;

  public abstract int getTag(URL paramURL)
    throws MalformedURLException, IOException;

  public abstract void setTag(URL paramURL, int paramInt)
    throws MalformedURLException, IOException;
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.PersistenceService
 * JD-Core Version:    0.6.0
 */