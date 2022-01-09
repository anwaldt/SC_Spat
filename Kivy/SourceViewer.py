#!/usr/bin/python3

from math import sin
from math import cos
from math import pi

import threading

from oscpy.server import OSCThreadServer

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
        size: min(self.size)*0.2, min(self.size)*0.2
        text: unicode(self.x), unicode(self.y)
        halign: 'right'
        valign: 'middle'
        text: root.text
        color: 0,0,0,1

''')


class SourceWidget(Widget):

    text          = StringProperty('as')
    color         = ListProperty([0.1, .8, .5])
    ellipse_size  = ListProperty([40,40])
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



# creating the App class
class SourceViewer(App):

    def build(self):

        self.main_layout =  GridLayout(rows=1,cols=2, row_default_height=40, spacing=50, size_hint_x=1,size_hint_y=1)

        self.xyl =  RelativeLayout()
        self.main_layout.add_widget(self.xyl)

        self.sources = []
        for i in range(16):
            s       = SourceWidget()
            s.color = [17/(i+1),(i+1)/8,(i%4)*100,0.5]
            s.ellipse_pos   = [i*50,i*50]
            s.pos   = [i*50,i*50]

            s.text  = str(i)
            self.sources.append(s)
            self.xyl.add_widget(s)

        self.button_grid = GridLayout(rows=9,cols=2, row_default_height=40, spacing=50, size_hint_x=1,size_hint_y=1)

        self.main_layout.add_widget(self.button_grid)

        self.select_buttons = []

        for i in range(16):

            self.select_buttons.append(Button(text='Source '+str(i)))

        for i in range(16):

            self.button_grid.add_widget(self.select_buttons[i])

            self.select_buttons[i].bind(on_press= partial(self.toggle_source,i))

        self.all_visible_button = Button(text='View All')
        self.button_grid.add_widget(self.all_visible_button)
        self.all_visible_button.bind(on_press= partial(self.all_visible))

        self.all_invisible_button = Button(text='View None')
        self.button_grid.add_widget(self.all_invisible_button)
        self.all_invisible_button.bind(on_press= partial(self.all_invisible))

        # self.server_thread = threading.Thread(target=self.server.serve_forever)
        # self.server_thread.deamon = True
        # self.server_thread.start()

        self.osc    =  OSCThreadServer()
        self.socket = self.osc.listen(address='0.0.0.0', port=9595, default=True)
        self.osc.bind(b'/source/aed', self.aed_handler)

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

        print(ind)

        s = self.sources[ind].visible
        print(s)
        if s==True:
            self.sources[ind].alpha=0.1
            self.sources[ind].visible = False
        if s==False:
            self.sources[ind].alpha=1
            self.sources[ind].visible = True

    def all_visible(self,button):

        for i in range(16):
            self.sources[i].alpha   = 1
            self.sources[i].visible = True

    def all_invisible(self,button):

        for i in range(16):
            self.sources[i].alpha   = 0.1
            self.sources[i].visible = False

# run the App
if __name__=='__main__':

    Window.fullscreen = True

    SourceViewer().run()
