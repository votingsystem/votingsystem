<!DOCTYPE html>
<html>
<head>
    <title>SPA</title>
    <vs:webscript dir='webcomponentsjs' file="webcomponents.min.js"/>
    <vs:webresource dir="polymer" file="polymer.html"/>
    <vs:webresource dir="core-scaffold" file="core-scaffold.html"/>
    <vs:webresource dir="core-toolbar" file="core-toolbar.html"/>
    <vs:webresource dir="core-menu" file="core-menu.html"/>
    <vs:webresource dir="paper-item" file="paper-item.html"/>
    <vs:webresource dir="core-icon" file="core-icon.html"/>
    <vs:webresource dir="core-animated-pages" file="core-animated-pages.html"/>
    <vs:webresource dir="core-animated-pages/transitions" file="slide-from-right.html"/>
    <vs:webresource dir="flatiron-director" file="flatiron-director.html"/>
    <vs:webresource dir="core-ajax" file="core-ajax.html"/>

    <style>
        body {
            font-family: "RobotoDraft";
            font-weight: 300;
        }
        core-animated-pages {
            width: 85%;
            height: 85%;
            -webkit-user-select: none;
            overflow: hidden;
        }
        core-animated-pages > * {
            border-radius: 5px;
            font-size: 50px;
            background-color: white;
        }
        body /deep/ core-toolbar {
            background-color: #03a9f4;
            color: #fff;
        }
        core-menu {
            color: #01579b;
            margin: 10px 0 0 0;
        }
        core-menu > paper-item {
            transition: all 300ms ease-in-out;
        }
        paper-item a {
            text-decoration: none;
            color: currentcolor;
            margin-left: 5px;
        }
        core-menu > paper-item.core-selected {
            background: #e1f5fe;
        }
        @media all and (max-width: 480px) {
            core-animated-pages {
                width: 100%;
                height: 100%;
            }
        }
    </style>
</head>
<body unresolved fullbleed='true'>
<template is="auto-binding" id="t">
    <core-ajax id="ajax" auto url="{{selectedPage.page.url}}" handleAs="document" on-core-response="{{onResponse}}">
    </core-ajax>

    <flatiron-director route="{{route}}" autoHash></flatiron-director>

    <core-scaffold id="scaffold">
        <nav>
            <core-menu id="menu" valueattr="hash"
                       selected="{{route}}"
                       selectedModel="{{selectedPage}}"
                       on-core-select="{{menuItemSelected}}" on-click="{{ajaxLoad}}">
                <template repeat="{{page, i in pages}}">
                    <paper-item hash="{{page.hash}}" noink>
                        <core-icon icon="label{{route != page.hash ? '-outline' : ''}}"></core-icon>
                        <a href="{{page.url}}">{{page.name}}</a>
                    </paper-item>
                </template>
            </core-menu>
        </nav>
        <!-- flex makes the bar span across the top of the main content area -->
        <core-toolbar tool flex>
            <!-- flex spaces this element and justifies the icons to the right-side -->
            <div flex>{{selectedPage.page.name}}</div>
            <core-icon-button icon="refresh"></core-icon-button>
            <core-icon-button icon="add"></core-icon-button>
        </core-toolbar>
        <div layout horizontal center-center fit>
            <core-animated-pages id="pages" selected="{{route}}" valueattr="hash"
                                 transitions="slide-from-right">
                <template repeat="{{page, i in pages}}">
                    <section hash="{{page.hash}}" layout vertical center-center>
                        <div style="max-width:100%;">Loading...</div>
                    </section>
                </template>
            </core-animated-pages>
        </div>
    </core-scaffold>
</template>
</body>
</html>
<asset:script>
    (function() {
    "use strict";

    var DEFAULT_ROUTE = 'one';

    var template = document.querySelector('#t');
    var ajax, pages, scaffold;
    var cache = {};

    template.pages = [
      {name: 'Shadow DOM 101', hash: 'one', url: 'http://cooins:8086/Cooins/userVS/search?mode=simplePage'},
      {name: 'Shadow DOM 201', hash: 'two', url: 'http://cooins:8086/Cooins/app/userVS/last?mode=simplePage'},
      {name: 'Shadow DOM 301', hash: 'three', url: 'http://www.html5rocks.com/en/tutorials/webcomponents/shadowdom-301/'},
      {name: 'Custom Elements', hash: 'four', url: 'http://www.html5rocks.com/en/tutorials/webcomponents/customelements/'}
    ];

    template.addEventListener('template-bound', function(e) {
      scaffold = document.querySelector('#scaffold');
      ajax = document.querySelector('#ajax');
      pages = document.querySelector('#pages');
      var keys = document.querySelector('#keys');

      // Allow selecting pages by num keypad. Dynamically add
      // [1, template.pages.length] to key mappings.
      var keysToAdd = Array.apply(null, template.pages).map(function(x, i) {
        return i + 1;
      }).reduce(function(x, y) {
        return x + ' ' + y;
      });
      keys.keys += ' ' + keysToAdd;

      this.route = this.route || DEFAULT_ROUTE; // Select initial route.
    });

    template.keyHandler = function(e, detail, sender) {
      // Select page by num key.
      var num = parseInt(detail.key);
      if (!isNaN(num) && num <= this.pages.length) {
        pages.selectIndex(num - 1);
        return;
      }

      switch (detail.key) {
        case 'left':
        case 'up':
          pages.selectPrevious();
          break;
        case 'right':
        case 'down':
          pages.selectNext();
          break;
        case 'space':
          detail.shift ? pages.selectPrevious() : pages.selectNext();
          break;
      }
    };

    template.menuItemSelected = function(e, detail, sender) {
      if (detail.isSelected) {

        // Need to wait one rAF so <core-ajax> has it's URL set.
        this.async(function() {
          if (!cache[ajax.url]) {
            ajax.go();
          }

          scaffold.closeDrawer();
        });

      }
    };

    template.ajaxLoad = function(e, detail, sender) {
      e.preventDefault(); // prevent link navigation.
    };

    template.onResponse = function(e, detail, sender) {
      var article = detail.response.querySelector('#article-content');

      var html = article.innerHTML;

      cache[ajax.url] = html; // Primitive caching by URL.

      this.injectBoundHTML(html, pages.selectedItem.firstElementChild);
    };

    })();
</asset:script>
<asset:deferredScripts/>