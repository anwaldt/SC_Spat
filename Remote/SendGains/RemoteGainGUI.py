#!/usr/bin/python3

from math import sin
from math import cos
from math import pi
from math import floor

import threading

import argparse
import yaml

import socket

from oscpy.server import OSCThreadServer
from oscpy.client import OSCClient

from kivy.core.window import Window
from kivy.config import ConfigParser

from kivy.uix.relativelayout import RelativeLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.button import Button
from kivy.uix.togglebutton import ToggleButton
from kivy.uix.label import Label
from kivy.uix.popup import Popup

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
    color         = ListProperty([0.5, 1, 0])
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

    box_pos  = ListProperty([100,100])
    box_size = ListProperty([60,800])
    color    = ListProperty([1,0.5,0])

    def __init__(self, ind, addr, port, **kwargs):

        self.source = 0

        self.idx = ind

        self.osc_client = OSCClient(addr, port)

        super(FaderWidget, self).__init__(**kwargs)


    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):

            gain = (touch.pos[1]-self.pos[1]) / self.size[1]

            self.osc_client.send_message(b'/send/gain', [self.source, self.idx, gain])
            #print([self.idx, self.source, gain])

    def on_touch_move(self, touch):
        if self.collide_point(*touch.pos):
            gain = (touch.pos[1]-self.pos[1]) / self.size[1]
            self.osc_client.send_message(b'/send/gain', [self.source, self.idx, gain])
            #print([self.idx, self.source, gain])

    def set_source(self,src):
        self.source=src

# creating the App class
class SourceViewer(App):

    # default config is created on first run
    def build_config(self, config):
        config.setdefaults(
            'self',
            {
            'n_channels': 16,
            'receive_port': '8989'
            })

        config.setdefaults(
            'renderbox',
            {
            'address': '127.0.0.1',
            'port': '8989'
             })

    def build(self):

        config =  self.config

        self.render_address = config.get('renderbox', 'address')
        self.render_port    = config.getint('renderbox', 'port')

        self.receive_port    = config.getint('self', 'receive_port')
        self.n_channels      = config.getint('self', 'n_channels')

        print(self.render_address)
        print(self.render_port)

        self.osc_client = OSCClient(self.render_address, self.render_port)

        # notify rendering server
        self.osc_client.send_message(b'/addlistener/gains',  [self.receive_port])


        n_channels = 16

        self.active_source = 0

        self.main_layout   = GridLayout(rows=1,cols=2, row_default_height=40, spacing=50, size_hint_x=1,size_hint_y=1)

        # a GridLayout for the faders
        self.xyl =  GridLayout(rows=3,col_default_width=80, cols =  n_channels,size_hint_x=1,size_hint_y=1)
        self.main_layout.add_widget(self.xyl)

        self.faders = []

        for i in range(n_channels):
            l = Label(text=str(i), font_size=30, bold=True, halign='center')
            self.xyl.add_widget(l)

        for i in range(n_channels):
            f = FaderWidget(i,self.render_address, self.render_port)

            if i<2:
                f.color = [1,0,0]

            else:
                f.color = [0,1,1]

            f.ids.bar.ellipse_pos   = [0,0]
            f.pos  = [i*80,100]

            f.text = str(i)

            self.faders.append(f)
            self.xyl.add_widget(f)

        self.button_grid = GridLayout(rows=(int(n_channels/2)+1),cols=2, row_default_height=40, spacing=50, size_hint_x=0.2,size_hint_y=1)

        self.main_layout.add_widget(self.button_grid)

        self.select_buttons = []

        for i in range(n_channels):

            self.select_buttons.append(Button(text='Source '+str(i)))

        for i in range(n_channels):

            self.button_grid.add_widget(self.select_buttons[i])
            self.select_buttons[i].background_color = (0, 0.5, 0.6,1)
            self.select_buttons[i].bind(on_press= partial(self.toggle_source,i))


        self.settings_button = Button(text='View All')
        self.button_grid.add_widget(self.settings_button)
        self.settings_button.bind(on_press= partial(self.open_settings))

        self.default_routing_button = Button(text='View None')
        self.button_grid.add_widget(self.default_routing_button)
        self.default_routing_button.bind(on_press= partial(self.default_routing))


        self.osc_server    =  OSCThreadServer()
        self.socket = self.osc_server.listen(address='0.0.0.0', port=self.receive_port, default=True)
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

    def open_settings(self,button):

        content = Button(text='Close me!')
        popup = Popup(content=content, auto_dismiss=False)

        content.bind(on_touch_down=popup.dismiss)

        # open the popup
        popup.open()


    def default_routing(self,button):

        x=1


# run the App
if __name__=='__main__':

    Window.size = (1920, 1080)

    SourceViewer().run()
