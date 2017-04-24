The goal of this research is to construct optimal dictionary to compress a set of strings.

To achieve this, an enhanced suffix array is created from original data (all input strings concatenated):

```
BuildSaMain
BuildRsaMain
BuildLcpMain
```
Then, a rating is assigned to each LCP-interval: it is measure of how good an interval is for inclusion in the dictionary.
The reasoning is: if text, that corresponds to LCP-interval is added to the dictionary, the following number of bytes is saved:
```
saved = (value(LCP-interval) - 3) * span(LCP-interval) 
```
but at the same time, value(LCP-interval) bytes are consumed from the size-limited dictionary.
So, the effectiveness, or rating, can be computed as
```
rating = (value(LCP-interval) - 3) * span(LCP-interval) / value(LCP-interval) 
```
which is close to span(LCP-interval) == number of times the substring that corresponds to LCP-interval appears. 

Then, LCP-intervals with highest rating are taken, e.g. with sorting all data first (but using top-k algo is faster):

```python
import numpy as np
intervals = np.memmap(
    'frequent-intervals',
    mode='r',
    dtype=[('offset', '>i8'),('length', '>i4'), ('rating', '>f4')]
)
_sorted = np.sort(intervals, order=['rating'], axis=0)
_sorted.tofile('frequent-intervals-sorted')
```
then, top-K intervals are converted to corresponding text and written to file
```python
import numpy as np
K = 32000
data = np.memmap(
    'frequent-intervals-sorted',
    mode='r',
    dtype=[('offset', '>i8'),('length', '>i4'), ('rating', '>f4')]
)
top_data = data[len(data) - K : len(data)]

text = np.memmap(
    'data',
    mode='r',
    dtype='u1'
)

def text_for(e):
    return [int(text[i]) for i in xrange(long(e["offset"]), long(e["offset"]) + long(e["length"]))]

import json

with open('raw_dict', 'wb') as outfile:
    r = []
    for i in xrange(0, K):
        if i % 100 == 0:
            print i
        r.append(text_for(top_data[i]))
        
    json.dump(r, outfile)
```

and finally, 
```
CommonSuperStringFinder
```
is run to blend these strings into optimized dictionary (see javadoc for description).
Other algorithms exist (e.g. by viewing this problem as Travelling salesman problem)
