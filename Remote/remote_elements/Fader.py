from kivy.uix.widget import Widget
from kivy.properties import ListProperty, StringProperty, BooleanProperty, NumericProperty

from oscpy.client import OSCClient


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
