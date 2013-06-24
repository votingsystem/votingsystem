// Place your Spring DSL code here
beans = {
	
	votingSystemApplicationContex(org.sistemavotacion.utils.VotingSystemApplicationContex) { bean ->
		bean.factoryMethod = 'getInstance'
	}
	
}
