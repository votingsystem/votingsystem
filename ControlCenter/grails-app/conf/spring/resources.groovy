// Place your Spring DSL code here
beans = {
	
	votingSystemApplicationContex(org.votingsystem.groovy.util.VotingSystemApplicationContex) { bean ->
		bean.factoryMethod = 'getInstance'
	}
	
}
