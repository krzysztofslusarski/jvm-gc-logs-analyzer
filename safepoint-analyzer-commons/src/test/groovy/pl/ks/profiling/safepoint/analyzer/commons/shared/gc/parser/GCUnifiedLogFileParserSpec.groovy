package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser

import spock.lang.Specification

import static pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser.GCLogCycleEntry.*

class GCUnifiedLogFileParserSpec extends Specification {
    def gcLogs = """[2020-12-21T01:04:47.091+0000][1778483.410s][debug][gc,ergo              ] GC(597760) Initiate concurrent cycle (concurrent cycle initiation requested)
[2020-12-21T01:04:47.091+0000][1778483.410s][info ][gc,start             ] GC(597760) Pause Young (Concurrent Start) (G1 Humongous Allocation)
[2020-12-21T01:04:47.091+0000][1778483.410s][info ][gc,task              ] GC(597760) Using 8 workers of 8 for evacuation
[2020-12-21T01:04:47.091+0000][1778483.410s][debug][gc,age               ] GC(597760) Desired survivor size 192937984 bytes, new threshold 15 (max threshold 15)
[2020-12-21T01:04:47.095+0000][1778483.414s][debug][gc,ergo              ] GC(597760) Running G1 Clear Card Table Task using 1 workers for 1 units of work for 210 regions.
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) Age table with threshold 15 (max threshold 15)
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   1:      94368 bytes,      94368 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   2:        688 bytes,      95056 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   3:       2768 bytes,      97824 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   4:      24752 bytes,     122576 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   5:      53688 bytes,     176264 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   6:        456 bytes,     176720 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   7:        288 bytes,     177008 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   8:        640 bytes,     177648 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age   9:        288 bytes,     177936 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age  10:        288 bytes,     178224 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age  11:        288 bytes,     178512 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age  12:        312 bytes,     178824 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age  13:        312 bytes,     179136 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age  14:        336 bytes,     179472 total
[2020-12-21T01:04:47.096+0000][1778483.414s][trace][gc,age               ] GC(597760) - age  15:        336 bytes,     179808 total
[2020-12-21T01:04:47.096+0000][1778483.414s][debug][gc,ergo              ] GC(597760) Running G1 Free Collection Set using 1 workers for collection set length 8
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 20 object size 1794400 start 0x00000006c2800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Dead humongous region 21 object size 1048592 start 0x00000006c2a00000 with remset 0 code roots 0 is marked 0 reclaim candidate 1 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 22 object size 1878240 start 0x00000006c2c00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 24 object size 1458432 start 0x00000006c3000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 31 object size 1048600 start 0x00000006c3e00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 32 object size 1712680 start 0x00000006c4000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 33 object size 1048600 start 0x00000006c4200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 35 object size 1656336 start 0x00000006c4600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 36 object size 1567176 start 0x00000006c4800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 38 object size 1048600 start 0x00000006c4c00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 39 object size 1048600 start 0x00000006c4e00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 48 object size 1561112 start 0x00000006c6000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 64 object size 1048600 start 0x00000006c8000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 65 object size 1048600 start 0x00000006c8200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 70 object size 1148152 start 0x00000006c8c00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 86 object size 1048600 start 0x00000006cac00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 89 object size 1342416 start 0x00000006cb200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 93 object size 1989496 start 0x00000006cba00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 97 object size 2097176 start 0x00000006cc200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 117 object size 1048600 start 0x00000006cea00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 118 object size 1048600 start 0x00000006cec00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 129 object size 2226936 start 0x00000006d0200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 138 object size 1048600 start 0x00000006d1400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 142 object size 2097176 start 0x00000006d1c00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 159 object size 1207936 start 0x00000006d3e00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 165 object size 1402784 start 0x00000006d4a00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 167 object size 1048600 start 0x00000006d4e00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 182 object size 1048600 start 0x00000006d6c00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 193 object size 2113320 start 0x00000006d8200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 203 object size 1048600 start 0x00000006d9600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 204 object size 2097176 start 0x00000006d9800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 212 object size 1048600 start 0x00000006da800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 214 object size 2097176 start 0x00000006dac00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 221 object size 2097176 start 0x00000006dba00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.096+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 225 object size 2097176 start 0x00000006dc200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 231 object size 2097176 start 0x00000006dce00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 242 object size 1487576 start 0x00000006de400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 254 object size 1048600 start 0x00000006dfc00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 257 object size 2097176 start 0x00000006e0200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 261 object size 1048600 start 0x00000006e0a00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 272 object size 2097176 start 0x00000006e2000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 274 object size 2097176 start 0x00000006e2400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 276 object size 2097176 start 0x00000006e2800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 280 object size 1048600 start 0x00000006e3000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 283 object size 3042616 start 0x00000006e3600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 289 object size 1048600 start 0x00000006e4200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 304 object size 2097176 start 0x00000006e6000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 308 object size 2366552 start 0x00000006e6800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 322 object size 2097176 start 0x00000006e8400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 328 object size 2097176 start 0x00000006e9000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 345 object size 2097176 start 0x00000006eb200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 348 object size 2097176 start 0x00000006eb800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 350 object size 1048600 start 0x00000006ebc00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 355 object size 2478488 start 0x00000006ec600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 360 object size 1048600 start 0x00000006ed000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 361 object size 1254376 start 0x00000006ed200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 363 object size 1048600 start 0x00000006ed600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 364 object size 1048600 start 0x00000006ed800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 390 object size 2097176 start 0x00000006f0c00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 393 object size 2097176 start 0x00000006f1200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 396 object size 2097176 start 0x00000006f1800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 398 object size 1187392 start 0x00000006f1c00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 399 object size 1048600 start 0x00000006f1e00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.415s][debug][gc,humongous         ] GC(597760) Live humongous region 400 object size 1048600 start 0x00000006f2000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 402 object size 2097176 start 0x00000006f2400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 460 object size 1522752 start 0x00000006f9800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 467 object size 1048600 start 0x00000006fa600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 468 object size 1368272 start 0x00000006fa800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 469 object size 1048600 start 0x00000006faa00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 470 object size 1048600 start 0x00000006fac00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 471 object size 1048600 start 0x00000006fae00000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 474 object size 1434192 start 0x00000006fb400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 475 object size 1048600 start 0x00000006fb600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 476 object size 1048600 start 0x00000006fb800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 484 object size 8345184 start 0x00000006fc800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 564 object size 8388632 start 0x0000000706800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 592 object size 8388632 start 0x000000070a000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 648 object size 3714728 start 0x0000000711000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 651 object size 4194328 start 0x0000000711600000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 666 object size 4194328 start 0x0000000713400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 682 object size 3968064 start 0x0000000715400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 684 object size 4194328 start 0x0000000715800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 690 object size 4194328 start 0x0000000716400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 744 object size 2100336 start 0x000000071d000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 746 object size 2097176 start 0x000000071d400000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 748 object size 2097176 start 0x000000071d800000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 768 object size 1048600 start 0x0000000720000000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 0
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,humongous         ] GC(597760) Live humongous region 769 object size 1048600 start 0x0000000720200000  with remset 1 code roots 0 is marked 0 reclaim candidate 0 type array 1
[2020-12-21T01:04:47.097+0000][1778483.416s][info ][gc,phases            ] GC(597760)   Pre Evacuate Collection Set: 0.4ms
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Prepare TLABs: 0.0ms
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Choose Collection Set: 0.0ms
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Humongous Register: 0.2ms
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Humongous Total: 88
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Humongous Candidate: 58
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Clear Claimed Marks: 0.3ms
[2020-12-21T01:04:47.097+0000][1778483.416s][info ][gc,phases            ] GC(597760)   Evacuate Collection Set: 2.3ms
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       GC Worker Start (ms):     Min: 1778483411.4, Avg: 1778483411.5, Max: 1778483411.5, Diff:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Ext Root Scanning (ms):   Min:  1.0, Avg:  1.0, Max:  1.2, Diff:  0.2, Sum:  8.3, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Thread Roots (ms):        Min:  0.0, Avg:  0.1, Max:  0.2, Diff:  0.2, Sum:  0.7, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       StringTable Roots (ms):   Min:  0.0, Avg:  0.1, Max:  0.2, Diff:  0.2, Sum:  1.1, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Universe Roots (ms):      Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       JNI Handles Roots (ms):   Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       ObjectSynchronizer Roots (ms): Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Management Roots (ms):    Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       SystemDictionary Roots (ms): Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       CLDG Roots (ms):          Min:  0.0, Avg:  0.1, Max:  0.7, Diff:  0.7, Sum:  0.7, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       JVMTI Roots (ms):         Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       CM RefProcessor Roots (ms): Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Wait For Strong CLD (ms): Min:  0.0, Avg:  0.4, Max:  0.5, Diff:  0.5, Sum:  3.2, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Weak CLD Roots (ms):      Min:  0.2, Avg:  0.3, Max:  0.5, Diff:  0.3, Sum:  2.4, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       SATB Filtering (ms):      Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Update RS (ms):           Min:  0.3, Avg:  0.6, Max:  1.1, Diff:  0.7, Sum:  4.8, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)       Processed Buffers:        Min: 1, Avg:  3.9, Max: 12, Diff: 11, Sum: 31, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)       Scanned Cards:            Min: 120, Avg: 252.6, Max: 392, Diff: 272, Sum: 2021, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)       Skipped Cards:            Min: 0, Avg:  0.2, Max: 1, Diff: 1, Sum: 2, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Scan HCC (ms):            Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Scan RS (ms):             Min:  0.0, Avg:  0.1, Max:  0.2, Diff:  0.2, Sum:  1.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)       Scanned Cards:            Min: 0, Avg: 66.4, Max: 127, Diff: 127, Sum: 531, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)       Claimed Cards:            Min: 0, Avg: 67.2, Max: 128, Diff: 128, Sum: 538, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)       Skipped Cards:            Min: 0, Avg: 392.9, Max: 474, Diff: 474, Sum: 3143, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Code Root Scanning (ms):  Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     AOT Root Scanning (ms):   skipped
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Object Copy (ms):         Min:  0.0, Avg:  0.2, Max:  0.4, Diff:  0.4, Sum:  1.7, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Termination (ms):         Min:  0.0, Avg:  0.2, Max:  0.2, Diff:  0.2, Sum:  1.4, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)       Termination Attempts:     Min: 1, Avg:  2.2, Max: 4, Diff: 3, Sum: 18, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     GC Worker Other (ms):     Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.1, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][debug][gc,phases            ] GC(597760)     GC Worker Total (ms):     Min:  2.2, Avg:  2.2, Max:  2.2, Diff:  0.0, Sum: 17.4, Workers: 8
[2020-12-21T01:04:47.097+0000][1778483.416s][trace][gc,phases            ] GC(597760)       GC Worker End (ms):       Min: 1778483413.6, Avg: 1778483413.6, Max: 1778483413.7, Diff:  0.0, Workers: 8
[2020-12-21T01:04:47.098+0000][1778483.416s][info ][gc,phases            ] GC(597760)   Post Evacuate Collection Set: 1.9ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Code Roots Fixup: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Clear Card Table: 0.4ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Reference Processing: 0.1ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Weak Processing: 0.1ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Merge Per-Thread State: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Code Roots Purge: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Redirty Cards: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Parallel Redirty (ms):    Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 8
[2020-12-21T01:04:47.098+0000][1778483.416s][trace][gc,phases            ] GC(597760)         Redirtied Cards:          Min: 0, Avg: 66.6, Max: 348, Diff: 348, Sum: 533, Workers: 8
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     DerivedPointerTable Update: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Free Collection Set: 0.4ms
[2020-12-21T01:04:47.098+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Free Collection Set Serial: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Young Free Collection Set (ms): Min:  0.0, Avg:  0.0, Max:  0.0, Diff:  0.0, Sum:  0.0, Workers: 1
[2020-12-21T01:04:47.098+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Non-Young Free Collection Set (ms): skipped
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Humongous Reclaim: 0.9ms
[2020-12-21T01:04:47.098+0000][1778483.416s][trace][gc,phases            ] GC(597760)       Humongous Reclaimed: 1
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Start New Collection Set: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Resize TLABs: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][debug][gc,phases            ] GC(597760)     Expand Heap After Collection: 0.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][info ][gc,phases            ] GC(597760)   Other: 1.0ms
[2020-12-21T01:04:47.098+0000][1778483.416s][info ][gc,heap              ] GC(597760) Eden regions: 7->0(1473)
[2020-12-21T01:04:47.098+0000][1778483.416s][info ][gc,heap              ] GC(597760) Survivor regions: 1->1(184)
[2020-12-21T01:04:47.098+0000][1778483.416s][info ][gc,heap              ] GC(597760) Old regions: 526->526
[2020-12-21T01:04:47.098+0000][1778483.416s][info ][gc,heap              ] GC(597760) Humongous regions: 137->136
[2020-12-21T01:04:47.098+0000][1778483.416s][info ][gc,metaspace         ] GC(597760) Metaspace: 93798K->93798K(1134592K)
[2020-12-21T01:04:47.098+0000][1778483.417s][info ][gc                   ] GC(597760) Pause Young (Concurrent Start) (G1 Humongous Allocation) 1338M->1324M(5120M) 6.363ms
[2020-12-21T01:04:47.098+0000][1778483.417s][info ][gc,cpu               ] GC(597760) User=0.02s Sys=0.01s Real=0.01s"""

    def "should parse gc log"() {
        given:
        GCUnifiedLogFileParser parser = new GCUnifiedLogFileParser();

        when:
        gcLogs.split("\n").each {
            parser.parseLine(it)
        }
        GCLogCycleEntry gcEntry = parser.fetchData().cycleEntries.head()

        then:
        gcEntry.timeStamp == 1778483.410G
        gcEntry.sequenceId == 597760
        gcEntry.phase == "Pause Young (Concurrent Start) (G1 Humongous Allocation)"
        gcEntry.aggregatedPhase == "Young collection - piggybacks"
        gcEntry.heapBeforeGCMb == 1338
        gcEntry.heapAfterGCMb == 1324
        gcEntry.heapSizeMb == 5120
        gcEntry.timeMs == 6.363
        phaseTimeMax(gcEntry, PRE_EVACUATE) == 0.4G
        subphaseTimeMax(gcEntry, PRE_PREPARE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, PRE_CHOOSE_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, PRE_HUMONGOUS_REGISTER) == 0.2G
        subphaseTimeMax(gcEntry, PRE_CLEAR_CLAIMED_MARKS) == 0.3G
        phaseTimeMax(gcEntry, EVACUATE) == 2.3G
        subphaseTimeMax(gcEntry, EVACUATE_EXT_ROOT_SCANNING) == 1.2G
        subphaseTimeMax(gcEntry, EVACUATE_UPDATE_RS) == 1.1G
        subphaseTimeMax(gcEntry, EVACUATE_SCAN_RS) == 0.2G
        subphaseTimeMax(gcEntry, EVACUATE_CODE_ROOT_SCANNING) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_AOT_ROOT_SCANNING) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_OBJECT_COPY) == 0.4G
        subphaseTimeMax(gcEntry, EVACUATE_TERMINATION) == 0.2G
        subphaseTimeMax(gcEntry, EVACUATE_GC_WORKER_OTHER) == 0.0G
        subphaseTimeMax(gcEntry, EVACUATE_GC_WORKER_TOTAL) == 2.2G
        phaseTimeMax(gcEntry, POST_EVACUATE) == 1.9G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_FIXUP) == 0.0G
        subphaseTimeMax(gcEntry, POST_CLEAR_CARD_TABLE) == 0.4G
        subphaseTimeMax(gcEntry, POST_REFERENCE_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, POST_WEAK_PROCESSING) == 0.1G
        subphaseTimeMax(gcEntry, POST_MERGE_PER_THREAD_STATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_CODE_ROOTS_PURGE) == 0.0G
        subphaseTimeMax(gcEntry, POST_REDIRTY_CARDS) == 0.0G
        subphaseTimeMax(gcEntry, POST_DERIVED_POINTER_TABLE_UPDATE) == 0.0G
        subphaseTimeMax(gcEntry, POST_FREE_COLLECTION_SET) == 0.4G
        subphaseTimeMax(gcEntry, POST_HUMONGOUS_RECLAIM) == 0.9G
        subphaseTimeMax(gcEntry, POST_START_NEW_COLLECTION_SET) == 0.0G
        subphaseTimeMax(gcEntry, POST_RESIZE_TLABS) == 0.0G
        subphaseTimeMax(gcEntry, POST_EXPAND_HEAP) == 0.0G
        phaseTimeMax(gcEntry, PHASE_OTHER) == 1.0G
        gcEntry.regionsBeforeGC[REGIONS_EDEN] == 7
        gcEntry.regionsBeforeGC[REGIONS_SURVIVOR] == 1
        gcEntry.regionsBeforeGC[REGIONS_OLD] == 526
        gcEntry.regionsBeforeGC[REGIONS_HUMONGOUS] == 137
        gcEntry.regionsAfterGC[REGIONS_EDEN] == 0
        gcEntry.regionsAfterGC[REGIONS_SURVIVOR] == 1
        gcEntry.regionsAfterGC[REGIONS_OLD] == 526
        gcEntry.regionsAfterGC[REGIONS_HUMONGOUS] == 136
        gcEntry.regionsMax[REGIONS_EDEN] == 1473
        gcEntry.regionsMax[REGIONS_SURVIVOR] == 184
        gcEntry.regionsMax[REGIONS_OLD] == null
        gcEntry.regionsMax[REGIONS_HUMONGOUS] == null
        gcEntry.regionsSizeAfterGC == null
        gcEntry.regionsWastedAfterGC == null
        gcEntry.liveHumongousSizes.size() == 87
        gcEntry.deadHumongousSizes.size() == 1
        !gcEntry.genuineCollection
        gcEntry.bytesInAges[1] == 94368
        gcEntry.bytesInAges[2] == 688
        gcEntry.bytesInAges[3] == 2768
        gcEntry.bytesInAges[4] == 24752
        gcEntry.bytesInAges[5] == 53688
        gcEntry.bytesInAges[6] == 456
        gcEntry.bytesInAges[7] == 288
        gcEntry.bytesInAges[8] == 640
        gcEntry.bytesInAges[9] == 288
        gcEntry.bytesInAges[10] == 288
        gcEntry.bytesInAges[11] == 288
        gcEntry.bytesInAges[12] == 312
        gcEntry.bytesInAges[13] == 312
        gcEntry.bytesInAges[14] == 336
        gcEntry.bytesInAges[15] == 336
        gcEntry.maxAge == 15
        gcEntry.desiredSurvivorSize == 192937984
        gcEntry.newTenuringThreshold == 15
        gcEntry.maxTenuringThreshold == 15
        !gcEntry.wasToSpaceExhausted
    }

    BigDecimal phaseTimeMax(GCLogCycleEntry gcEntry, String phase) {
        return gcEntry.subPhasesTime[phase]
    }

    BigDecimal subphaseTimeMax(GCLogCycleEntry gcEntry, String subphase) {
        return gcEntry.subPhasesTime["|______${subphase}" as String]
    }

}
