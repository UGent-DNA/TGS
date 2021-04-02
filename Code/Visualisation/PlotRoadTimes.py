import matplotlib.pyplot as plt
import numpy as np 
import matplotlib.cm as cm

typ = "Traffic"

t0 = np.loadtxt(typ+"_times_plan0.txt", unpack=True)
t1 = np.loadtxt(typ+"_times_plan1.txt", unpack=True)
t2 = np.loadtxt(typ+"_times_planG.txt", unpack=True)

t_max = np.maximum(np.max(t0), np.max(t1))
t_min = np.minimum(np.min(t0), np.min(t1))

t0_av = np.mean(t0)
t1_av = np.mean(t1)
t2_av = np.mean(t2)

print(t0_av,t1_av,t2_av)


t0_avs = []
t1_avs = []
t2_avs = []


# s = (t_max-t_min)/2
# t_av = (t0_av+t1_av+t2_av)/3

# color_map = cm.get_cmap('viridis')

# ax0 = plt.subplot(311)
# ax0.set_xlim(0,t_max)
# ax0.text(14000, 1700, "Random planning", fontsize='large', fontweight='bold')

# ax1 = plt.subplot(312, sharex = ax0)
# ax1.text(14000, 1600, "Local planning", fontsize='large', fontweight='bold')

# ax2 = plt.subplot(313, sharex = ax0)
# ax2.text(14000, 1600, "Global planning", fontsize='large', fontweight='bold')
# plt.xlabel("Total time spent in traffic per truck (s)")
# # plt.suptitle()


# n, bins, patches = ax0.hist(t0, range=(0,t_max), bins = 50)

# for i, p in enumerate(patches):
# 	t = p.xy[0]
# 	c = 0.5 + (t-t_av)/s
# 	plt.setp(p, 'facecolor', color_map(c))


# n, bins, patches = ax1.hist(t1, range=(0,t_max), bins = 50)

# for i, p in enumerate(patches):
# 	t = p.xy[0]
# 	c = 0.5 + (t-t_av)/s
# 	plt.setp(p, 'facecolor', color_map(c))


# n, bins, patches = ax2.hist(t2, range=(0,t_max), bins = 50)

# for i, p in enumerate(patches):
# 	t = p.xy[0]
# 	c = 0.5 + (t-t_av)/s
# 	plt.setp(p, 'facecolor', color_map(c))


b=100

xlim = t_max

plt.hist(t0, range=(0,xlim), bins = b, histtype ='step', label='Random planning')
plt.hist(t1, range=(0,xlim), bins = b, histtype ='step', label='Local planning')
plt.hist(t2, range=(0,xlim), bins = b, histtype ='step', label='Global planning')
plt.legend(loc='best')
plt.xlim(0,xlim)
plt.xlabel("Total time spent in traffic per truck (s)")

plt.show()