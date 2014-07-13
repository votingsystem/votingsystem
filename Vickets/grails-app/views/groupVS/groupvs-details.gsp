<polymer-element name="groupvs-details" attributes="groupvs index isHero">
    <template>
        <style>
        .view {
            background-color: tomato;
        }
        </style>
        <div class="view" flex vertical center center-justified layout hero-id="groupvs-{{index}}" hero?="{{isHero}}">
        <span cross-fade>{{groupvs.name}}</span>
    </div>
    </template>
    <script>
        Polymer('groupvs-details', {
            isSelected: false
        })
    </script>
</polymer-element>