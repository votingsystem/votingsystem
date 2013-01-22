package javax.jnlp;

import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;

public abstract interface PrintService
{
  public abstract PageFormat getDefaultPage();

  public abstract PageFormat showPageFormatDialog(PageFormat paramPageFormat);

  public abstract boolean print(Pageable paramPageable);

  public abstract boolean print(Printable paramPrintable);
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.PrintService
 * JD-Core Version:    0.6.0
 */