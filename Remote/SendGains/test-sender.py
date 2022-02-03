#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Jan 26 01:16:48 2022

@author: anwaldt
"""


from oscpy.server import OSCThreadServer
from oscpy.client import OSCClient

osc_client = OSCClient('127.0.0.1', 8989)
 
receive_address = '127.0.0.1'
receive_port    = 9898

osc_client.send_message(b'/addlistener/gains', [receive_port])

osc_client.send_message(b'/send/gain', [0, 0, 1.5])
