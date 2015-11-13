<%@ page contentType="text/html; charset=UTF-8" %>

<dom-module id="search-bar">
  <style>
    :host * {
      box-sizing: border-box;
    }

    :host(.search-on) {
      left: 0;
      background: inherit;
      z-index: 1001;
    }

    #search {
      position: relative;
      cursor: pointer;
    }

    .vs-navbar-icon:hover {
      color: #fba131;
      cursor: pointer;
    }

    #search[show] {
      position: absolute;
      width: 100%;
      height: 100%;
      left: 0;
      top: 0;
      padding: 0 16px;
      background: #ba0011;
    }

    #search input {
      display: none;
      font-size: 15px;
      width: 100%;
      padding: 10px;
      border: 0;
      border-radius: 2px;
      -webkit-appearance: none;
    }

    #search[show] input {
      display: block;
    }

    #search input:focus {
      outline: 0;
    }
  </style>
  <template>
    <content></content>
      <div id="search" class="horizontal layout center" show$="{{showingSearch}}" on-tap="toggleSearch">
        <i id="searchIcon" class="fa fa-search" style="margin:0px 20px 0px 0px; font-size: 1.3em;" ></i>
        <form on-submit="performSearch" class="flex">
          <input type="search" id="query" value="{{query::keyup}}" autocomplete="off" placeholder="${msg.searchLbl}" on-blur="clearSearch">
        </form>
      </div>
  </template>
</dom-module>

<script>
  Polymer({
    is: 'search-bar',
    properties: {
      query: { type: String, notify: true },
      showingSearch: { type: Boolean, value: false },
      noSearch: { type: Boolean, value: false }
    },
    observers: [
      'updateSearchDisplay(showingSearch)'
    ],
    listeners: {
      keyup: 'hotkeys'
    },
    toggleSearch: function(e) {
      if (e) {
        e.stopPropagation();
      }
      if (e.target === this.$.query) {
        return;
      }
      this.showingSearch = !this.showingSearch;
    },
    clearSearch: function() {
      this.showingSearch = false;
    },
    updateSearchDisplay: function(showingSearch) {
      if (showingSearch) {
        this.classList.add('search-on');
        this.$.searchIcon.className = "fa fa-times vs-navbar-icon"
      } else {
        this.classList.remove('search-on');
        this.$.searchIcon.className = "fa fa-search vs-navbar-icon"
      }
    },
    hotkeys: function(e) {
      // ESC
      if (e.keyCode === 27 && Polymer.dom(e).rootTarget === this.$.query) {
        this.showingSearch = false;
      }
    },
    performSearch: function(e) {
      e.preventDefault();
      this.fire('search-request', {query: this.query});
    }
  });
</script>