package javax.jnlp;

public abstract interface ServiceManagerStub
{
  public abstract Object lookup(String paramString)
    throws UnavailableServiceException;

  public abstract String[] getServiceNames();
}

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.ServiceManagerStub
 * JD-Core Version:    0.6.0
 */