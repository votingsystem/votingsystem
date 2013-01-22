/*    */ package javax.jnlp;
/*    */ 
/*    */ public final class ServiceManager
/*    */ {
/* 23 */   private static ServiceManagerStub _stub = null;
/*    */ 
/*    */   public static Object lookup(String paramString)
/*    */     throws UnavailableServiceException
/*    */   {
/* 41 */     if (_stub != null) {
/* 42 */       return _stub.lookup(paramString);
/*    */     }
/* 44 */     throw new UnavailableServiceException("uninitialized");
/*    */   }
/*    */ 
/*    */   public static String[] getServiceNames()
/*    */   {
/* 52 */     if (_stub != null) {
/* 53 */       return _stub.getServiceNames();
/*    */     }
/* 55 */     return null;
/*    */   }
/*    */ 
/*    */   public static synchronized void setServiceManagerStub(ServiceManagerStub paramServiceManagerStub)
/*    */   {
/* 70 */     if (_stub == null)
/* 71 */       _stub = paramServiceManagerStub;
/*    */   }
/*    */ }

/* Location:           /home/jgzornoza/Descargas/jdk1.6.0_37/sample/jnlp/servlet/jnlp/
 * Qualified Name:     javax.jnlp.ServiceManager
 * JD-Core Version:    0.6.0
 */