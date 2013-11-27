// Place your Spring DSL code here
beans = {
	
	applicationContextHolder(org.votingsystem.util.ApplicationContextHolder) { bean ->
		bean.factoryMethod = 'getInstance'
	 }
}
