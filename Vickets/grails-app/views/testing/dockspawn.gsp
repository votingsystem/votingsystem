<!DOCTYPE html>
<!--http://www.dockspawn.com/docs/dock_spawn/DockManager.html-->
<html>
<head>
  	<title>Test DockSpawn</title>
    <meta name="layout" content="main" />
    <meta name="layout" content="main" />
    <link rel="stylesheet" href="${resource(dir: 'dockspawn/font-awesome/css', file: 'font-awesome.css')}" type="text/css"/>
    <asset:stylesheet src="vickets.css"/>
    <link rel="stylesheet" href="${resource(dir: 'dockspawn', file: 'testDockSpawn.css')}" type="text/css"/>
    <link rel="stylesheet" href="${resource(dir: 'dockspawn', file: 'dock-manager.css')}" type="text/css"/>
    <script src="${resource(dir: 'dockspawn', file: 'dockspawn.js')}" type="text/javascript"></script>

</head>
<body>

<div id="dockContainer" style="visibility:hidden;">
    <div id="my_dock_manager" class="my-dock-manager"></div>
    <div id="solution_window" caption="Solution Explorer" icon="icon-circle-arrow-right" class="solution-window">
        <ul>
            <li id="solution_window_1">
                <a href="#">Test Project</a>
                <ul>
                    <li id="phtml_2"><a href="#">Source File 1</a></li>
                    <li id="phtml_3"><a href="#">Header File 1</a></li>
                </ul>
            </li>
            <li id="solution_window_2">
                <a href="#">My Project 2</a>
            </li>
        </ul>
    </div>
    <div id="properties_window" caption="Properties" class="properties-window"></div>
    <div id="problems_window" caption="Problems" class="problems-window"></div>
    <div id="editor1_window" caption="Last Events" class="editor1-window editor-host"></div>
    <div id="editor2_window" caption="Alerts" class="editor2-window editor-host"></div>
    <div id="infovis" caption="Dock Tree Visualizer" class="editor2-window editor-host"></div>
    <div id="output_window" caption="Output" class="output-window editor-host"></div>
    <div id="toolbox_window" caption="Toolbox" class="toolbox-window">
        <ul>
            <li id="toolbox_window_1"><a href="#">Tool 1</a></li>
            <li id="toolbox_window_2"><a href="#">Tool 2</a></li>
            <li id="toolbox_window_3"><a href="#">Tool 3</a></li>
            <li id="toolbox_window_4"><a href="#">Tool 4</a></li>
            <li id="toolbox_window_5"><a href="#">Tool 5</a></li>
        </ul>
    </div>
    <div id="outline_window" caption="Outline" class="outline-window">
    </div>
</div>

<div class="form-group has-error">
    <i style="color:#6c0404; font-size: 4em;" class="fa fa-cog fa-spin"></i>
</div>
</body>
</html>
<asset:script>

    function submitForm(form) {
        var result = document.getElementById("testTime").getValidatedTime()
        console.log("result: " + result)
        return false
    }

       window.onload = function()
        {
            // Convert a div to the dock manager.  Panels can then be docked on to it
            var divDockManager = document.getElementById("my_dock_manager");
            var dockManager = new dockspawn.DockManager(divDockManager);
            dockManager.initialize();
            // Let the dock manager element fill in the entire screen
            var onResized = function(e)
            {
                dockManager.resize(window.innerWidth - (divDockManager.clientLeft + divDockManager.offsetLeft), window.innerHeight - (divDockManager.clientTop + divDockManager.offsetTop));
            }
            window.onresize = onResized;
            onResized(null);

            // Convert existing elements on the page into "Panels".
            // They can then be docked on to the dock manager
            // Panels get a titlebar and a close button, and can also be
            // converted to a floating dialog box which can be dragged / resized
            var solution = new dockspawn.PanelContainer(document.getElementById("solution_window"), dockManager);
            var properties = new dockspawn.PanelContainer(document.getElementById("properties_window"), dockManager);
            var toolbox = new dockspawn.PanelContainer(document.getElementById("toolbox_window"), dockManager);
            var outline = new dockspawn.PanelContainer(document.getElementById("outline_window"), dockManager);
            var problems = new dockspawn.PanelContainer(document.getElementById("problems_window"), dockManager);
            var output = new dockspawn.PanelContainer(document.getElementById("output_window"), dockManager);
            var editor1 = new dockspawn.PanelContainer(document.getElementById("editor1_window"), dockManager);
            var editor2 = new dockspawn.PanelContainer(document.getElementById("editor2_window"), dockManager);
            var infovis = new dockspawn.PanelContainer(document.getElementById("infovis"), dockManager);

            // Dock the panels on the dock manager
            var documentNode = dockManager.context.model.documentManagerNode;
            var outlineNode = dockManager.dockLeft(documentNode, outline, 0.15);
            var solutionNode = dockManager.dockFill(outlineNode, solution);
            var propertiesNode = dockManager.dockDown(outlineNode, properties, 0.6);
            var outputNode = dockManager.dockDown(documentNode, output, 0.2);
            var problemsNode = dockManager.dockRight(outputNode, problems, 0.40);
            var toolboxNode = dockManager.dockRight(documentNode, toolbox, 0.20);
            var editor1Node = dockManager.dockFill(documentNode, editor1);
            var editor2Node = dockManager.dockFill(documentNode, editor2);
            var infovisNode = dockManager.dockFill(documentNode, infovis);

            document.getElementById('dockContainer').style.visibility = 'visible'
        }

    $(document).ready(function(){
        $('#testForm').submit(function(event){event.preventDefault();});
        $("#submitButton").click(function(){});
    });

</asset:script>
<asset:deferredScripts/>