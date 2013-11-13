// Place your Spring DSL code here
beans = {
	
	applicationContextHolder(org.votingsystem.simulation.ApplicationContextHolder) { bean ->
		bean.factoryMethod = 'getInstance'
	 }
}
