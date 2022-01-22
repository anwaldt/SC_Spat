#!/usr/bin/python3

from math import sin
from math import cos
from math import pi
from math import floor

import threading

from oscpy.server import OSCThreadServer
from oscpy.client import OSCClient

from kivy.core.window import Window

from kivy.uix.relativelayout import RelativeLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.button import Button
from kivy.uix.togglebutton import ToggleButton

from kivy.app import App
from kivy.base import runTouchApp

from kivy.uix.widget import Widget
from kivy.properties import ListProperty, StringProperty, BooleanProperty, NumericProperty
from kivy.lang import Builder
from kivy.graphics import Color, Ellipse

from functools import partial

Builder.load_string('''
<FaderWidget>:
    size_hint_x: None
    size_hint_y: None
    size: self.box_size
    pos: self.box_pos
    canvas.before:
        Color:
            rgb: self.color
        RoundedRectangle:
            id: frame
            size: self.box_size
            pos: self.pos
            radius: [(20, 20), (20, 20), (20, 20), (20, 20)]
    FaderBarWidget:
        id: bar

<FaderBarWidget>:
    canvas:
        Color:
            rgb: self.color
            a: self.alpha
        Ellipse:
            pos: self.ellipse_pos
            size: self.ellipse_size

''')


class FaderBarWidget(Widget):

    text          = StringProperty('as')
    color         = ListProperty([1, 1, 0])
    ellipse_size  = ListProperty([60,60])
    ellipse_pos   = ListProperty([22,22])
    pos           = ListProperty([22,22])
    alpha         = NumericProperty(1)
    visible       = True

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            self.on_touch_move(touch)

    def on_touch_move(self, touch):
        x=1
        #self.ellipse_pos = [touch.pos[0], touch.pos[1]]
        #self.pos = [touch.pos[0], touch.pos[1]]

class FaderWidget(Widget):

    def __init__(self, ind, **kwargs):

        self.source = 0

        self.idx = ind

        self.osc_client = OSCClient("127.0.0.1", 8989)

        super(FaderWidget, self).__init__(**kwargs)

    box_pos  = 100,100
    box_size = 60,900
    color    = 1,0,0

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):

            gain = (touch.pos[1]-self.pos[1]) / self.size[1]

            self.osc_client.send_message(b'/send/gain', [self.source, self.idx, gain])
            print([self.idx, self.source, gain])

    def on_touch_move(self, touch):
        if self.collide_point(*touch.pos):
            gain = (touch.pos[1]-self.pos[1]) / self.size[1]
            self.osc_client.send_message(b'/send/gain', [self.source, self.idx, gain])
            print([self.idx, self.source, gain])

    def set_source(self,src):
        self.source=src

# creating the App class
class SourceViewer(App):

    def build(self):

        self.active_source = 0;

        self.main_layout   = GridLayout(rows=1,cols=2, row_default_height=40, spacing=50, size_hint_x=1,size_hint_y=1)

        self.xyl =  RelativeLayout()
        self.main_layout.add_widget(self.xyl)

        self.faders = []

        for i in range(16):
            f = FaderWidget(i)
            c = 1+floor(i/2)

            f.color = ((0.33*c)%1, (0.5+c*0.4)%1 ,(0.2*c)%1)
            f.ellipse_pos   = [i*50,i*50]
            f.pos   = [i*100,100]

            f.text  = str(i)
            self.faders.append(f)
            self.xyl.add_widget(f)

        self.button_grid = GridLayout(rows=9,cols=2, row_default_height=40, spacing=50, size_hint_x=0.2,size_hint_y=1)

        self.main_layout.add_widget(self.button_grid)

        self.select_buttons = []

        for i in range(16):

            self.select_buttons.append(Button(text='Source '+str(i)))

        for i in range(16):

            self.button_grid.add_widget(self.select_buttons[i])
            self.select_buttons[i].background_color = (0, 0.5, 0.6,1)
            self.select_buttons[i].bind(on_press= partial(self.toggle_source,i))


        #self.all_visible_button = Button(text='View All')
        #self.button_grid.add_widget(self.all_visible_button)
        #self.all_visible_button.bind(on_press= partial(self.all_visible))

        #self.all_invisible_button = Button(text='View None')
        #self.button_grid.add_widget(self.all_invisible_button)
        #self.all_invisible_button.bind(on_press= partial(self.all_invisible))



        self.osc_server    =  OSCThreadServer()
        self.socket = self.osc_server.listen(address='0.0.0.0', port=9596, default=True)
        self.osc_server.bind(b'/send/level', self.send_level_handler)

        self.toggle_source(0,self.select_buttons[0])

        return self.main_layout

    def send_level_handler(self, *values):

        src    = values[0]
        if src==self.active_source:
            dest   = values[1]
            gain   = values[2]

            # scale to fader box
            y = self.faders[dest].pos[1] + gain * self.faders[dest].size[1] -  (0.5*self.faders[dest].ids.bar.ellipse_size[1])

            self.faders[dest].ids.bar.ellipse_pos = [self.faders[dest].pos[0],y]


    def toggle_source(self,ind,button):

        self.active_source = ind;

        # dim other buttons
        for b in self.select_buttons:
            b.background_color = (0.3, 0.3, 0.3,1)
        # highlight active
        self.select_buttons[ind].background_color = (0, 0.1, 0.9,1)

        #set source index
        for f in self.faders:
            f.set_source(ind)

    def all_visible(self,button):

        for i in range(16):
            self.sources[i].alpha   = 1
            self.sources[i].visible = True
            self.select_buttons[i].background_color = (0, 0.5, 0.6,1)

    def all_invisible(self,button):

        for i in range(16):
            self.sources[i].alpha   = 0.1
            self.sources[i].visible = False
            self.select_buttons[i].background_color = (1, 0, 0,0.3)

# run the App
if __name__=='__main__':

    #Window.fullscreen = True

    SourceViewer().run()
