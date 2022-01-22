#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thu Aug 30 23:35:31 2018

@author: anwaldt
"""
from pythonosc import dispatcher

from pythonosc import udp_client
from pythonosc import osc_server
from pythonosc import osc_message_builder as omb

osc_client =[]

class OscSender:



    def __init__(self):

        self.osc_client  = udp_client.SimpleUDPClient("127.0.0.1", 57120)


    def SendMsg(self,msg):

        self.osc_client.send(msg)

        # only needed for SSR
        # osc_client.send_message("/poll",' ')

class OscServer:

    def __init__(self):


        self.dispatcher = dispatcher.Dispatcher()

        self.dispatcher.map("/vbap/azim", self.vbap_azim_handler)

        server = osc_server.ThreadingOSCUDPServer(( "0.0.0.0", 9494), dispatcher)

        print("Serving on {}".format(server.server_address))
