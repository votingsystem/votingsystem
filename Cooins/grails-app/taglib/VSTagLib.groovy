/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class VSTagLib {
    static namespace = "vs"

    def webcss = { attrs ->
        out << "<link rel='stylesheet' href='${g.resource(dir: 'bower_components/' + attrs.dir, file: attrs.file)}' type='text/css'/>"
    }

    def webscript = { attrs ->
        out << "<script src='${g.resource(dir: 'bower_components/' + attrs.dir, file: attrs.file)}'></script>"
    }

    def webresource = { attrs ->
        out << "<link rel='import' href='${g.resource(dir: 'bower_components/' + attrs.dir, file: attrs.file)}'/>"
    }

    def webcomponent = { attrs ->
        out << "<link rel='import' href='${createLink(controller:'element', params:[element:attrs.path])}'/>"
    }
}
