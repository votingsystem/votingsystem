// Place your Spring DSL code here
beans = {
    votingSystemApplicationContex(org.votingsystem.util.ApplicationContextHolder) { bean ->
        bean.factoryMethod = 'getInstance'
    }

}
