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
<LfoMainWidget>:
    canvas:
        Color:
            rgb: 0.1,0.1,0.1
        RoundedRectangle:
            id: frame
            size: self.bg_size
            pos: self.bg_pos
            radius: [(20, 20), (20, 20), (20, 20), (20, 20)]

<SourceWidget>:
    canvas:
        Color:
            rgb: self.color
            a: self.alpha
        Ellipse:
            pos: self.ellipse_pos
            size: self.ellipse_size
    Label:
        pos: root.pos[0]+20,root.pos[1]+20
        size: root.size[0],root.size[1]
        text: unicode(self.x), unicode(self.y)
        halign: 'right'
        valign: 'middle'
        text: root.text
        color: 0,0,0,1

<FaderWidget>:
    size_hint_x: 0.2
    size_hint_y: 0.2
    size: self.box_size
    pos: self.box_pos
    canvas.before:
        Color:
            rgb: self.color
        RoundedRectangle:
            id: frame
            size: [self.size[0]*0.9, self.size[1]*0.9]
            pos: self.pos
            radius: [(20, 20), (20, 20), (20, 20), (20, 20)]
    FaderBarWidget:
        id: bar

    Label:
        pos: root.pos[0],root.pos[1]
        size: root.size[0],root.size[1]
        text: unicode(self.x), unicode(self.y)
        halign: 'left'
        valign: 'top'
        text: root.text
        color: 1,1,1,1

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

    color         = ListProperty([0, 0, 0])
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
    box_size = ListProperty([80,400])
    color    = ListProperty([0.3, 0.3, 0.3])
    text     = StringProperty('SSS')


    def __init__(self, path, addr, port, **kwargs):

        self.index  = 0

        self.min    = 0
        self.max    = 10
        self.path   = path
        self.source = 0

        self.osc_client = OSCClient(addr, port)

        super(FaderWidget, self).__init__(**kwargs)


    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            gain = ((touch.pos[1]-self.pos[1]) / self.size[1]) * self.max
            self.osc_client.send_message(bytes(self.path,encoding='utf8') , [self.index, gain])
            #print([self.idx, self.source, gain])

    def on_touch_move(self, touch):
        if self.collide_point(*touch.pos):
            gain = ((touch.pos[1]-self.pos[1]) / self.size[1]) * self.max
            self.osc_client.send_message(bytes(self.path,encoding='utf8'), [self.index, gain])
            #print([self.idx, self.source, gain])

    def set_source(self,src):
        self.source=src

    def on_receive(self, *values):


        src    = values[0]

        if src==self.index:

            gain   = values[1]

            # print(gain)

            y = self.pos[1] + (gain/self.max) * self.size[1] -  (0.5*self.ids.bar.ellipse_size[1])

            self.ids.bar.ellipse_pos = [self.pos[0] + (self.size[0]/2) - (self.ids.bar.ellipse_size[0]/2), y] #[self.faders[dest].pos[0],y]

    def set_index(self, ind):
        self.index = ind;

class LfoMainWidget(Widget):
    bg_size  = ListProperty([300,1000])
    bg_pos   = ListProperty([10,10])

class SourceWidget(Widget):

    text          = StringProperty('')
    color         = ListProperty([0.3, .8, .5])
    ellipse_size  = ListProperty([40,40])
    ellipse_pos   = ListProperty([22,22])
    pos           = ListProperty([22,22])
    alpha         = NumericProperty(0.5)
    visible       = True

    def on_touch_down(self, touch):
        if self.collide_point(*touch.pos):
            self.on_touch_move(touch)

    def on_touch_move(self, touch):
        x=1
        #self.ellipse_pos = [touch.pos[0], touch.pos[1]]
        #self.pos = [touch.pos[0], touch.pos[1]]



# creating the App class
class SourceViewer(App):


    # default config is created on first run
    def build_config(self, config):
        config.setdefaults(
            'self',
            {
            'n_sources': 14,
            'receive_port': '8988'
            })

        config.setdefaults(
            'renderbox',
            {
            'address': '127.0.0.1',
            'port': '8989'
             })

    def build(self):

        config              = self.config

        self.active_lfo     = 0

        self.render_address = config.get('renderbox', 'address')
        self.render_port    = config.getint('renderbox', 'port')

        self.receive_port   = config.getint('self', 'receive_port')
        self.n_sources      = config.getint('self', 'n_sources')

        print(self.render_address)
        print(self.render_port)

        self.osc_client = OSCClient(self.render_address, self.render_port)

        # notify rendering server
        self.osc_client.send_message(b'/addlistener/position',  [self.receive_port])


        self.main_layout =  GridLayout(rows=1,cols=4, row_default_height=40, spacing=50, size_hint_x=1,size_hint_y=1)

        self.lfo_layout = GridLayout(rows=4,cols=2,size_hint_x=0.15)
        self.main_layout.add_widget(self.lfo_layout)


        self.lfo_dur_fader = FaderWidget('/lfo/dur', self.render_address, self.render_port,size_hint_x=0.2,size_hint_y=0.2)
        self.lfo_dur_fader.box_pos = 10,50
        self.lfo_dur_fader.text = "DURATION"
        self.lfo_layout.add_widget(self.lfo_dur_fader)

        self.lfo_dir_fader = FaderWidget('/lfo/dir', self.render_address, self.render_port)
        self.lfo_dir_fader.box_pos = 100,50
        self.lfo_dir_fader.text = "DIRECTION"
        self.lfo_layout.add_widget(self.lfo_dir_fader)


        self.lfo_gain_fader = FaderWidget('/lfo/gain', self.render_address, self.render_port)
        self.lfo_gain_fader.box_pos = 10,500
        self.lfo_gain_fader.text = "GAIN"
        self.lfo_layout.add_widget(self.lfo_gain_fader)

        self.lfo_off_fader = FaderWidget('/lfo/offset', self.render_address, self.render_port)
        self.lfo_off_fader.box_pos = 100,500
        self.lfo_off_fader.text = "OFFSET"
        self.lfo_layout.add_widget(self.lfo_off_fader)

        self.lfo_activate_button = Button(text='LFO',size_hint_x=0.1,size_hint_y=0.05)
        self.lfo_layout.add_widget(self.lfo_activate_button)
        self.lfo_activate_button.bind(on_press= partial(self.lfo_activate_button_callback))

        self.lfo_deactivate_button = Button(text='DIRECT',size_hint_x=0.1,size_hint_y=0.05)
        self.lfo_layout.add_widget(self.lfo_deactivate_button)
        self.lfo_deactivate_button.bind(on_press= partial(self.lfo_deactivate_button_callback))

        self.lfo_button_grid = GridLayout(rows=(int(self.n_sources/2)), row_default_height=40, spacing=50, size_hint_x=0.1,size_hint_y=1)
        self.main_layout.add_widget(self.lfo_button_grid)

        self.lfo_buttons = []

        for i in range(int(self.n_sources/2)):

            self.lfo_buttons.append(Button(text='LFO '+str(i)))

        for i in range(int(self.n_sources/2)):

            self.lfo_button_grid.add_widget(self.lfo_buttons[i])
            self.lfo_buttons[i].background_color = (0, 0.5, 0.6,1)
            self.lfo_buttons[i].bind(on_press= partial(self.select_lfo,i))





        self.xy_layout =  RelativeLayout(size_hint_x=0.6)
        self.main_layout.add_widget(self.xy_layout)

        self.sources = []
        for i in range(self.n_sources):
            s       = SourceWidget()
            c = 1+floor(i/2)
            print(c)
            s.color = ((0.33*c)%1, (0.5+c*0.4)%1 ,(0.2*c)%1)
            s.ellipse_pos   = [i*50,i*50]
            s.pos   = [i*50,i*50]

            s.text  = str(i)
            self.sources.append(s)
            self.xy_layout.add_widget(s)

        self.src_button_grid = GridLayout(rows=(int(self.n_sources/2)+1),cols=2, row_default_height=40, spacing=50, size_hint_x=0.2,size_hint_y=1)

        self.main_layout.add_widget(self.src_button_grid)

        self.select_buttons = []

        for i in range(self.n_sources):

            self.select_buttons.append(Button(text='Source '+str(i)))

        for i in range(self.n_sources):

            self.src_button_grid.add_widget(self.select_buttons[i])
            self.select_buttons[i].background_color = (0, 0.5, 0.6,1)
            self.select_buttons[i].bind(on_press= partial(self.toggle_source,i))

        self.all_visible_button = Button(text='View All')
        self.src_button_grid.add_widget(self.all_visible_button)
        self.all_visible_button.bind(on_press= partial(self.all_visible))

        self.all_invisible_button = Button(text='View None')
        self.src_button_grid.add_widget(self.all_invisible_button)
        self.all_invisible_button.bind(on_press= partial(self.all_invisible))

        # self.server_thread = threading.Thread(target=self.server.serve_forever)
        # self.server_thread.deamon = True
        # self.server_thread.start()

        self.osc_server  =  OSCThreadServer()
        self.socket      = self.osc_server.listen(address='0.0.0.0', port=self.receive_port, default=True)
        self.osc_server.bind(b'/source/aed', self.aed_handler)


        # bind all LFO controls
        self.osc_server.bind(bytes(self.lfo_dur_fader.path ,encoding='utf8'), self.lfo_dur_fader.on_receive)
        self.osc_server.bind(bytes(self.lfo_dir_fader.path ,encoding='utf8'), self.lfo_dir_fader.on_receive)
        self.osc_server.bind(bytes(self.lfo_gain_fader.path ,encoding='utf8'), self.lfo_gain_fader.on_receive)
        self.osc_server.bind(bytes(self.lfo_off_fader.path ,encoding='utf8'), self.lfo_off_fader.on_receive)


        return self.main_layout

    def aed_handler(self, *values):
        idx = values[0]
        a   = values[1]
        e   = pi/2-values[2]
        d   = values[3]

        # plain coordinates
        y = d*cos(a)*sin(e)
        x = d*sin(a)*sin(e)
        z = d*cos(e)


        # canvas coordinates
        X = x*200 +600
        Y = y*200 +400


        self.sources[idx].ellipse_pos = [X,Y]


    def toggle_source(self,ind,button):

        s = self.sources[ind].visible

        if s==True:
            self.sources[ind].alpha=0.1
            self.sources[ind].visible = False
            self.select_buttons[ind].background_color = (1, 0.5, 0,0.3)

        if s==False:
            self.sources[ind].alpha=1
            self.sources[ind].visible = True
            self.select_buttons[ind].background_color = (0, 0.5, 0.6,1)

    def all_visible(self,button):

        for i in range(self.n_sources):
            self.sources[i].alpha   = 1
            self.sources[i].visible = True
            self.select_buttons[i].background_color = (0, 0.5, 0.6,1)

    def all_invisible(self,button):

        for i in range(self.n_sources):
            self.sources[i].alpha   = 0.1
            self.sources[i].visible = False
            self.select_buttons[i].background_color = (1, 0, 0,0.3)


    def select_lfo(self,ind,button):


        for b in self.lfo_buttons:

            b.background_color = (1, 0.5, 0,0.3)

        self.lfo_buttons[ind].background_color = (1, 1, 0,1)

        self.lfo_dur_fader.set_index(ind)
        self.lfo_dir_fader.set_index(ind)
        self.lfo_gain_fader.set_index(ind)
        self.lfo_off_fader.set_index(ind)

    def lfo_activate_button_callback(self,button):
        self.osc_client.send_message(b'/encoder/mode',  [self.active_lfo, b'lfo'])

    def lfo_deactivate_button_callback(self,button):
        self.osc_client.send_message(b'/encoder/mode',  [self.active_lfo, b'direct'])
# run the App
if __name__=='__main__':

    #Window.fullscreen = True

    SourceViewer().run()
